package com.mantledillusion.vaadin.cotton.model;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.essentials.reflection.TypeEssentials;
import com.mantledillusion.injection.hura.AnnotationValidator;
import com.mantledillusion.injection.hura.annotation.Inject.SingletonMode;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Predefinable.Singleton;
import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

/**
 * A pool of {@link Validator}s forming a validation context that is applicable
 * on a model.
 *
 * @param <ModelType>
 *            The root type of the data model the {@link ValidationContext} can
 *            be applied on.
 */
public final class ValidationContext<ModelType> {

	static final class PreEvaluateValidator implements AnnotationValidator<PreEvaluated, Class<?>> {

		@Override
		public void validate(PreEvaluated annotationInstance, Class<?> annotatedElement) throws Exception {
			if (!Validator.class.isAssignableFrom(annotatedElement)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The @" + PreEvaluated.class.getSimpleName() + " annotation can only be used on "
								+ Validator.class.getSimpleName() + " implementations; the type '"
								+ annotatedElement.getSimpleName() + "' however is not.");
			}
		}
	}

	/**
	 * An error registry to register found validation errors at.
	 *
	 * @param <ModelType>
	 *            The root type of the data model the
	 *            {@link ValidationErrorRegistry} can hold validation errors of.
	 */
	public static final class ValidationErrorRegistry<ModelType> {

		final Map<ModelProperty<?, ?>, Set<String>> errorMessages = new IdentityHashMap<>();

		ValidationErrorRegistry() {
		}

		/**
		 * Adds the given error message to the registry for all the given
		 * {@link ModelProperty}s.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param errorMsgId
		 *            The error message (or localizable error message id) to use;
		 *            <b>not</b> allowed to be null.
		 * @param property
		 *            The first {@link ModelProperty} to register the error for;
		 *            <b>not</b> allowed to be null.
		 * @param additionalProperties
		 *            The second -&gt; nth {@link ModelProperty} to register the error
		 *            for; might be null or contain null.
		 */
		@SuppressWarnings("unchecked")
		public void addError(String errorMsgId, ModelProperty<ModelType, ?> property,
				ModelProperty<ModelType, ?>... additionalProperties) {
			if (property == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Unable to add error message for a null property.");
			} else if (errorMsgId == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Unable to add null-message for the property " + property);
			}
			errorMsgId = WebEnv.localize(errorMsgId);
			add(property, errorMsgId);
			if (additionalProperties != null) {
				for (ModelProperty<ModelType, ?> prop : additionalProperties) {
					if (prop != null) {
						add(prop, errorMsgId);
					}
				}
			}
		}

		private void add(ModelProperty<ModelType, ?> property, String message) {
			if (!this.errorMessages.containsKey(property)) {
				this.errorMessages.put(property, new HashSet<>());
			}
			this.errorMessages.get(property).add(message);
		}
	}

	private final Injector injector;
	private final Map<Class<? extends Validator<ModelType>>, Pair<Validator<ModelType>, Set<Pair<Class<? extends Validator<ModelType>>, Boolean>>>> validators = new HashMap<>();
	private final List<Class<? extends Validator<ModelType>>> executionList = new ArrayList<>();

	private ValidationContext(Injector injector, Set<Class<? extends Validator<ModelType>>> validators) {

		this.injector = injector;

		// REGISTER ALL VALIDATORS RECURSIVELY
		for (Class<? extends Validator<ModelType>> validator : validators) {
			register(validator);
		}

		// KAHN'S ALGORITHM PREPERATION
		Set<Class<? extends Validator<ModelType>>> unrequisitedNodes = new HashSet<>();
		Map<Class<? extends Validator<ModelType>>, Set<Class<? extends Validator<ModelType>>>> graph = new HashMap<>();
		Map<Class<? extends Validator<ModelType>>, Set<Class<? extends Validator<ModelType>>>> inversedGraph = new HashMap<>();
		for (Class<? extends Validator<ModelType>> validator : this.validators.keySet()) {
			if (!inversedGraph.containsKey(validator)) {
				inversedGraph.put(validator, new HashSet<>());
			}
			if (!graph.containsKey(validator)) {
				graph.put(validator, new HashSet<>());
			}
			if (this.validators.get(validator).getRight().isEmpty()) {
				unrequisitedNodes.add(validator);
			} else {
				for (Pair<Class<? extends Validator<ModelType>>, Boolean> prerequisite : this.validators.get(validator)
						.getRight()) {
					inversedGraph.get(validator).add(prerequisite.getLeft());
					if (!graph.containsKey(prerequisite.getLeft())) {
						graph.put(prerequisite.getLeft(), new HashSet<>());
					}
					graph.get(prerequisite.getLeft()).add(validator);
				}
			}
		}

		// KAHN'S ALGORITHM
		if (unrequisitedNodes.isEmpty()) {
			throw new WebException(HttpErrorCodes.HTTP508_LOOP_DETECTED,
					"No validator could be found in the set of validators and their prerequisites that is no requisite to another validator; "
							+ "that indicates that the validators depend on each other bilaterally, forming a circlic dependency, which is not resolvable.");
		}

		while (!unrequisitedNodes.isEmpty()) {
			Class<? extends Validator<ModelType>> n = unrequisitedNodes.iterator().next();
			unrequisitedNodes.remove(n);
			if (validators.contains(n)) {
				this.executionList.add(n);
			}
			Iterator<Class<? extends Validator<ModelType>>> mIter = graph.get(n).iterator();
			while (mIter.hasNext()) {
				Class<? extends Validator<ModelType>> m = mIter.next();
				inversedGraph.get(m).remove(n);
				if (inversedGraph.get(m).isEmpty()) {
					unrequisitedNodes.add(m);
				}
				mIter.remove();
			}
			graph.remove(n);
		}

		if (!graph.isEmpty()) {
			throw new WebException(HttpErrorCodes.HTTP508_LOOP_DETECTED,
					"There is at least one circlic dependency between the validators, which is not resolvable.");
		}
	}

	private void register(Class<? extends Validator<ModelType>> validator) {
		if (!this.validators.containsKey(validator)) {
			Validator<ModelType> instance = this.injector.instantiate(validator);
			Set<Pair<Class<? extends Validator<ModelType>>, Boolean>> prerequisites = new HashSet<>();
			this.validators.put(validator, Pair.of(instance, prerequisites));

			Map<TypeVariable<?>, Type> args = TypeUtils.getTypeArguments(validator, Validator.class);
			for (Class<?> type : TypeEssentials.getSuperClassesAnnotatedWith(validator, PreEvaluated.class)) {
				PreEvaluated preevaluate = type.getAnnotation(PreEvaluated.class);
				for (PreEvaluated.Prerequisite prerequisite : preevaluate.value()) {
					if (args.equals(TypeUtils.getTypeArguments(prerequisite.value(), Validator.class))) {
						@SuppressWarnings("unchecked")
						Class<? extends Validator<ModelType>> prerequisiteType = (Class<? extends Validator<ModelType>>) prerequisite
								.value();

						if (prerequisiteType == validator) {
							throw new WebException(HttpErrorCodes.HTTP508_LOOP_DETECTED,
									"The validator " + validator.getSimpleName()
											+ " requires itself as prerequisite, which is not possible.");
						}
						prerequisites.add(Pair.of(prerequisiteType, prerequisite.requiredResult()));
						register(prerequisiteType);
					} else {
						throw new WebException(HttpErrorCodes.HTTP907_ILLEGAL_STRUCTURING,
								"The validator type " + prerequisite.value().getSimpleName()
										+ " is a prerequisite to the validator type " + validator.getSimpleName()
										+ ", but they do not share the same model/property type combination.");
					}
				}
			}
		}
	}

	ValidationContext.ValidationErrorRegistry<ModelType> validate(ModelHandler<ModelType> handler) {
		Map<Class<? extends Validator<ModelType>>, ValidationContext.ValidationErrorRegistry<ModelType>> results = new HashMap<>();
		ValidationContext.ValidationErrorRegistry<ModelType> result = new ValidationContext.ValidationErrorRegistry<>();

		for (Class<? extends Validator<ModelType>> validatorType : this.executionList) {
			if (evaluate(handler, validatorType, results)) {
				result.errorMessages.putAll(results.get(validatorType).errorMessages);
			}
		}
		return result;
	}

	private boolean evaluate(ModelHandler<ModelType> handler, Class<? extends Validator<ModelType>> validatorType,
			Map<Class<? extends Validator<ModelType>>, ValidationContext.ValidationErrorRegistry<ModelType>> results) {
		Pair<Validator<ModelType>, Set<Pair<Class<? extends Validator<ModelType>>, Boolean>>> validator = this.validators
				.get(validatorType);
		boolean doEvaluate = true;
		for (Pair<Class<? extends Validator<ModelType>>, Boolean> prerequisite : validator.getRight()) {
			if (!results.containsKey(prerequisite.getLeft())) {
				evaluate(handler, prerequisite.getLeft(), results);
			}
			doEvaluate &= (results.get(prerequisite.getLeft()).errorMessages.isEmpty() == prerequisite.getRight());
		}
		if (doEvaluate && !results.containsKey(validatorType)) {
			ValidationContext.ValidationErrorRegistry<ModelType> result = new ValidationContext.ValidationErrorRegistry<>();
			validator.getLeft().validate(handler, result);
			results.put(validatorType, result);
		}
		return doEvaluate;
	}

	/**
	 * Begins a builder to build a {@link ValidationContext}.
	 * <p>
	 * Supplying no {@link Injector} to use for {@link Validator} implementation
	 * instantiation causes a new {@link Injector} to be created with the given
	 * {@link Singleton}s as {@link SingletonMode#GLOBAL} {@link Singleton}s.
	 * 
	 * @param <ModelType>
	 *            The model type to begin a {@link ValidationContextBuilder} for.
	 * @param validator
	 *            The first {@link Validator} to give to the builder; <b>not</b>
	 *            allowed to be null.
	 * @param singletons
	 *            The {@link Singleton}s to make available as
	 *            {@link SingletonMode#GLOBAL} {@link Singleton}s to all of the
	 *            {@link Validator} implementations given to the returned
	 *            {@link ValidationContextBuilder}; might be null or contain nulls,
	 *            both is ignored.
	 * @return A new {@link ValidationContextBuilder} instance; never null
	 */
	public static <ModelType> ValidationContextBuilder<ModelType> of(Class<? extends Validator<ModelType>> validator,
			Singleton... singletons) {
		return of(Injector.of(), validator);
	}

	/**
	 * Begins a builder to build a {@link ValidationContext} with a specified
	 * {@link Injector}.
	 * <p>
	 * Supplying an {@link Injector} to use for {@link Validator} implementation
	 * instantiation enables the {@link Validator} instances to retrieve singletons
	 * out of the given {@link Injector}s contexts.
	 * 
	 * @param <ModelType>
	 *            The model type to begin a {@link ValidationContextBuilder} for.
	 * @param injector
	 *            The {@link Injector} to inject the {@link Validator}
	 *            implementations with; might <b>not</b> be null.
	 * @param validator
	 *            The first {@link Validator} to give to the builder; <b>not</b>
	 *            allowed to be null.
	 * @return A new {@link ValidationContextBuilder} instance; never null
	 */
	public static <ModelType> ValidationContextBuilder<ModelType> of(Injector injector,
			Class<? extends Validator<ModelType>> validator) {
		if (injector == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot start a validation context builder with a null injector.");
		}
		ValidationContext.ValidationContextBuilder<ModelType> context = new ValidationContext.ValidationContextBuilder<>(
				injector);
		context.and(validator);
		return context;
	}

	/**
	 * A builder for {@link ValidationContext}s.
	 *
	 * @param <ModelType>
	 *            The root type of the data model the {@link ValidationContext} that
	 *            is being build will have.
	 */
	public static final class ValidationContextBuilder<ModelType> {

		private final Injector injector;
		private Set<Class<? extends Validator<ModelType>>> validators = new HashSet<>();

		private ValidationContextBuilder(Injector injector) {
			this.injector = injector;
		}

		/**
		 * Adds the given {@link Validator} to the {@link ValidationContext} to build.
		 * <p>
		 * The {@link Validator} will be instantiated using an {@link Injector}, meaning
		 * injection is enabled for the given {@link Validator} type.
		 * 
		 * @param validatorType
		 *            The {@link Validator} to add; <b>not</b> allowed to be null.
		 * @return this
		 */
		public ValidationContext.ValidationContextBuilder<ModelType> and(
				Class<? extends Validator<ModelType>> validatorType) {
			if (validatorType == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot add a null validator type.");
			}
			this.validators.add(validatorType);
			return this;
		}

		/**
		 * Builds a new {@link ValidationContext} on every invocation.
		 * 
		 * @return A new {@link ValidationContext} with the values currently set; never
		 *         null
		 */
		public ValidationContext<ModelType> build() {
			return new ValidationContext<>(this.injector, this.validators);
		}
	}
}