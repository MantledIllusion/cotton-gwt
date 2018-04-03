package com.mantledillusion.vaadin.cotton.model;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.mantledillusion.essentials.reflection.TypeEssentials;
import com.mantledillusion.injection.hura.AnnotationValidator;
import com.mantledillusion.injection.hura.annotation.Inject.SingletonMode;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Predefinable.Singleton;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

/**
 * A pool of {@link PropertyValidator}s forming a validation context that is
 * applicable on a model.
 *
 * @param <ModelType>
 *            The root type of the data model the {@link ValidationContext} can
 *            be applied on.
 */
public final class ValidationContext<ModelType> {

	static final class PreEvaluatedValidator implements AnnotationValidator<PreEvaluated, Class<?>> {

		@Override
		public void validate(PreEvaluated annotationInstance, Class<?> annotatedElement) throws Exception {
			if (!PropertyValidator.class.isAssignableFrom(annotatedElement)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The @" + PreEvaluated.class.getSimpleName() + " annotation can only be used on "
								+ PropertyValidator.class.getSimpleName() + " implementations; the type '"
								+ annotatedElement.getSimpleName() + "' however is not.");
			}
		}
	}

	private final Injector injector;
	private final Map<Class<? extends PropertyValidator<ModelType>>, Pair<PropertyValidator<ModelType>, Set<Pair<Class<? extends PropertyValidator<ModelType>>, Boolean>>>> validators = new HashMap<>();
	private final List<Class<? extends PropertyValidator<ModelType>>> executionList = new ArrayList<>();

	private ValidationContext(Injector injector, Set<Class<? extends PropertyValidator<ModelType>>> validators) {

		this.injector = injector;

		// REGISTER ALL VALIDATORS RECURSIVELY
		for (Class<? extends PropertyValidator<ModelType>> validator : validators) {
			register(validator);
		}

		// KAHN'S ALGORITHM PREPERATION
		Set<Class<? extends PropertyValidator<ModelType>>> unrequisitedNodes = new HashSet<>();
		Map<Class<? extends PropertyValidator<ModelType>>, Set<Class<? extends PropertyValidator<ModelType>>>> graph = new HashMap<>();
		Map<Class<? extends PropertyValidator<ModelType>>, Set<Class<? extends PropertyValidator<ModelType>>>> inversedGraph = new HashMap<>();
		for (Class<? extends PropertyValidator<ModelType>> validator : this.validators.keySet()) {
			if (!inversedGraph.containsKey(validator)) {
				inversedGraph.put(validator, new HashSet<>());
			}
			if (!graph.containsKey(validator)) {
				graph.put(validator, new HashSet<>());
			}
			if (this.validators.get(validator).getRight().isEmpty()) {
				unrequisitedNodes.add(validator);
			} else {
				for (Pair<Class<? extends PropertyValidator<ModelType>>, Boolean> prerequisite : this.validators
						.get(validator).getRight()) {
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
			Class<? extends PropertyValidator<ModelType>> n = unrequisitedNodes.iterator().next();
			unrequisitedNodes.remove(n);
			if (validators.contains(n)) {
				this.executionList.add(n);
			}
			Iterator<Class<? extends PropertyValidator<ModelType>>> mIter = graph.get(n).iterator();
			while (mIter.hasNext()) {
				Class<? extends PropertyValidator<ModelType>> m = mIter.next();
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

	private void register(Class<? extends PropertyValidator<ModelType>> validator) {
		if (!this.validators.containsKey(validator)) {
			PropertyValidator<ModelType> instance = this.injector.instantiate(validator);
			Set<Pair<Class<? extends PropertyValidator<ModelType>>, Boolean>> prerequisites = new HashSet<>();
			this.validators.put(validator, Pair.of(instance, prerequisites));

			Map<TypeVariable<?>, Type> args = TypeUtils.getTypeArguments(validator, PropertyValidator.class);
			for (Class<?> type : TypeEssentials.getSuperClassesAnnotatedWith(validator, PreEvaluated.class)) {
				PreEvaluated preevaluate = type.getAnnotation(PreEvaluated.class);
				for (PreEvaluated.Prerequisite prerequisite : preevaluate.value()) {
					if (args.equals(TypeUtils.getTypeArguments(prerequisite.value(), PropertyValidator.class))) {
						@SuppressWarnings("unchecked")
						Class<? extends PropertyValidator<ModelType>> prerequisiteType = (Class<? extends PropertyValidator<ModelType>>) prerequisite
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

	void validate(ModelHandler<ModelType> handler, ValidationErrorRegistry<ModelType> registry) {
		Map<Class<? extends PropertyValidator<ModelType>>, ValidationErrorRegistry<ModelType>> results = new HashMap<>();

		for (Class<? extends PropertyValidator<ModelType>> validatorType : this.executionList) {
			if (evaluate(handler, validatorType, results)) {
				registry.addAll(results.get(validatorType));
			}
		}
	}

	private boolean evaluate(ModelHandler<ModelType> handler,
			Class<? extends PropertyValidator<ModelType>> validatorType,
			Map<Class<? extends PropertyValidator<ModelType>>, ValidationErrorRegistry<ModelType>> results) {
		Pair<PropertyValidator<ModelType>, Set<Pair<Class<? extends PropertyValidator<ModelType>>, Boolean>>> validator = this.validators
				.get(validatorType);
		boolean doEvaluate = true;
		for (Pair<Class<? extends PropertyValidator<ModelType>>, Boolean> prerequisite : validator.getRight()) {
			if (!results.containsKey(prerequisite.getLeft())) {
				evaluate(handler, prerequisite.getLeft(), results);
			}
			doEvaluate &= (results.get(prerequisite.getLeft()).getValidity().isValid() == prerequisite.getRight());
		}
		if (doEvaluate && !results.containsKey(validatorType)) {
			ValidationErrorRegistry<ModelType> result = new ValidationErrorRegistry<>();
			validator.getLeft().validate(handler, result);
			results.put(validatorType, result);
		}
		return doEvaluate;
	}

	/**
	 * Begins a builder to build a {@link ValidationContext}.
	 * <p>
	 * Supplying no {@link Injector} to use for {@link PropertyValidator}
	 * implementation instantiation causes a new {@link Injector} to be created with
	 * the given {@link Singleton}s as {@link SingletonMode#GLOBAL}
	 * {@link Singleton}s.
	 * 
	 * @param <ModelType>
	 *            The model type to begin a {@link ValidationContextBuilder} for.
	 * @param validator
	 *            The first {@link PropertyValidator} to give to the builder;
	 *            <b>not</b> allowed to be null.
	 * @param singletons
	 *            The {@link Singleton}s to make available as
	 *            {@link SingletonMode#GLOBAL} {@link Singleton}s to all of the
	 *            {@link PropertyValidator} implementations given to the returned
	 *            {@link ValidationContextBuilder}; might be null or contain nulls,
	 *            both is ignored.
	 * @return A new {@link ValidationContextBuilder} instance; never null
	 */
	public static <ModelType> ValidationContextBuilder<ModelType> of(
			Class<? extends PropertyValidator<ModelType>> validator, Singleton... singletons) {
		return of(Injector.of(), validator);
	}

	/**
	 * Begins a builder to build a {@link ValidationContext} with a specified
	 * {@link Injector}.
	 * <p>
	 * Supplying an {@link Injector} to use for {@link PropertyValidator}
	 * implementation instantiation enables the {@link PropertyValidator} instances
	 * to retrieve singletons out of the given {@link Injector}s contexts.
	 * 
	 * @param <ModelType>
	 *            The model type to begin a {@link ValidationContextBuilder} for.
	 * @param injector
	 *            The {@link Injector} to inject the {@link PropertyValidator}
	 *            implementations with; might <b>not</b> be null.
	 * @param validator
	 *            The first {@link PropertyValidator} to give to the builder;
	 *            <b>not</b> allowed to be null.
	 * @return A new {@link ValidationContextBuilder} instance; never null
	 */
	public static <ModelType> ValidationContextBuilder<ModelType> of(Injector injector,
			Class<? extends PropertyValidator<ModelType>> validator) {
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
		private Set<Class<? extends PropertyValidator<ModelType>>> validators = new HashSet<>();

		private ValidationContextBuilder(Injector injector) {
			this.injector = injector;
		}

		/**
		 * Adds the given {@link PropertyValidator} to the {@link ValidationContext} to
		 * build.
		 * <p>
		 * The {@link PropertyValidator} will be instantiated using an {@link Injector},
		 * meaning injection is enabled for the given {@link PropertyValidator} type.
		 * 
		 * @param validatorType
		 *            The {@link PropertyValidator} to add; <b>not</b> allowed to be
		 *            null.
		 * @return this
		 */
		public ValidationContext.ValidationContextBuilder<ModelType> and(
				Class<? extends PropertyValidator<ModelType>> validatorType) {
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