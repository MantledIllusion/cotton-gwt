package com.mantledillusion.vaadin.cotton.viewpresenter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mantledillusion.injection.hura.PhasedProcessor;
import com.mantledillusion.injection.hura.Processor;
import com.mantledillusion.injection.hura.Processor.Phase;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprintTemplate;
import com.mantledillusion.essentials.reflection.TypeEssentials;
import com.mantledillusion.injection.hura.AnnotationValidator;
import com.mantledillusion.injection.hura.BeanAllocation;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Inspector;
import com.mantledillusion.injection.hura.Injector.TemporalInjectorCallback;
import com.mantledillusion.injection.hura.annotation.Construct;
import com.mantledillusion.injection.hura.annotation.Define;
import com.mantledillusion.injection.hura.annotation.Process;
import com.mantledillusion.vaadin.cotton.CottonUI.UserChangeAnnouncementEvent;
import com.mantledillusion.vaadin.cotton.CottonUI.UserChangeType;
import com.mantledillusion.vaadin.cotton.EventBusSubscriber;
import com.mantledillusion.vaadin.cotton.UrlResourceRegistry;
import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;

/**
 * Basic super type for a view.
 * <p>
 * NOTE: Should be injected, since the {@link Injector} handles the instance's
 * life cycles.
 * <p>
 * Might be controlled by an {@link Presenter} implementation
 * using @{@link Presented} on the {@link View} implementing type; see the
 * documentation of @{@link Presented} for reference.
 */
public abstract class View extends Composite {

	private static final long serialVersionUID = 1L;

	// #########################################################################################################################################
	// ############################################################# ADDRESSABLE ###############################################################
	// #########################################################################################################################################

	static class AddressableValidator implements AnnotationValidator<Addressed, Class<?>> {

		@Override
		public void validate(Addressed annotationInstance, Class<?> annotatedElement) throws Exception {
			if (!View.class.isAssignableFrom(annotatedElement)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The @" + Addressed.class.getSimpleName() + " annotation can only be used on "
								+ View.class.getSimpleName() + " implementations; the type '"
								+ annotatedElement.getSimpleName() + "' however is not.");
			} else if (Frame.class.isAssignableFrom(annotatedElement)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"Despite being a " + View.class.getSimpleName() + " implementation, the @"
								+ Addressed.class.getSimpleName() + " annotation cannot be used on "
								+ Frame.class.getSimpleName() + " implementations, since " + Frame.class.getSimpleName()
								+ "s cannot be addressed directly.");
			}

			UrlResourceRegistry.checkUrlPattern(annotationInstance.value());

			Set<String> redirects = new HashSet<>();
			for (Addressed.Redirect redirect : annotationInstance.redirects()) {
				UrlResourceRegistry.checkUrlPattern(redirect.value());
				if (annotationInstance.value().equals(redirect.value())) {
					throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
							"Cannot redirect the view '" + annotatedElement.getSimpleName() + "' at URL '"
									+ annotationInstance.value() + "' to itself.");
				} else if (redirects.contains(redirect.value())) {
					throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
							"Cannot redirect from '" + redirect.value() + "' to the view '"
									+ annotatedElement.getSimpleName() + "' at URL '" + annotationInstance.value()
									+ "' more than once.");
				}
			}
		}
	}

	// #########################################################################################################################################
	// ############################################################## RESTRICTED ###############################################################
	// #########################################################################################################################################

	static class RestrictedValidator implements AnnotationValidator<Restricted, Class<?>> {

		@Override
		public void validate(Restricted annotationInstance, Class<?> annotatedElement) throws Exception {
			if (!View.class.isAssignableFrom(annotatedElement)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The @" + Restricted.class.getSimpleName() + " annotation can only be used on "
								+ View.class.getSimpleName() + " implementations; the type '"
								+ annotatedElement.getSimpleName() + "' however is not.");
			}
		}
	}

	static class RestrictedInspector extends EventBusSubscriber
			implements Inspector<Restricted, Class<? extends View>> {

		@Construct
		private RestrictedInspector() {
		}
		
		@Override
		public void inspect(Object bean, Restricted annotationInstance, Class<? extends View> annotatedElement,
				TemporalInjectorCallback callback) throws Exception {
			List<Class<?>> restrictions = TypeEssentials.getSuperClassesAnnotatedWith(annotatedElement,
					Restricted.class);

			if (!restrictions.isEmpty() && !WebEnv.isLoggedIn()) {
				throw new WebException(HttpErrorCodes.HTTP403_FORBIDDEN, "The view '" + annotatedElement.getSimpleName()
						+ "' requires a user to be logged in, but there is none.");
			}

			Set<String> requiredUserRights = new HashSet<>();
			for (Class<?> type : restrictions) {
				Restricted restricted = type.getAnnotation(Restricted.class);
				if (restricted.value() != null) {
					for (String requiredUserRight : restricted.value()) {
						if (requiredUserRight != null) {
							requiredUserRights.add(requiredUserRight);
						}
					}
				}
			}

			if (!requiredUserRights.isEmpty() && !WebEnv.areAllowed(requiredUserRights)) {
				throw new WebException(HttpErrorCodes.HTTP403_FORBIDDEN,
						"The view '" + annotatedElement.getSimpleName() + "' requires the user to have the rights "
								+ requiredUserRights + ", but one ore more are missing.");
			}
		}

		@Subscribe
		private void handleUserChangeAnnounced(UserChangeAnnouncementEvent event) {
			if (event.getChangeType() == UserChangeType.LOGOUT) {
				event.refreshAfterChange();
			}
		}
	}

	// #########################################################################################################################################
	// ################################################################ PRESENT ################################################################
	// #########################################################################################################################################

	static class PresentValidator implements AnnotationValidator<Presented, Class<?>> {

		@Override
		public void validate(Presented annotationInstance, Class<?> annotatedElement) throws Exception {
			if (!View.class.isAssignableFrom(annotatedElement)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The @" + Presented.class.getSimpleName() + " annotation can only be used on "
								+ View.class.getSimpleName() + " implementations; the type '"
								+ annotatedElement.getSimpleName() + "' however is not.");
			}
		}
	}

	// #########################################################################################################################################
	// ########################################################## COMPONENT REGISTRY ###########################################################
	// #########################################################################################################################################

	/**
	 * Temporarily active registry for {@link AbstractComponent}s on an {@link View}
	 * that are active (fire component events that the controlling {@link Presenter}
	 * has to react on).
	 * <P>
	 * May only be used during the initialization of the {@link View} it is given
	 * to.
	 */
	protected final class TemporalActiveComponentRegistry {

		private final Map<String, List<AbstractComponent>> activeComponents = new HashMap<>();
		private boolean canRegister = true;

		/**
		 * Registers the given {@link AbstractComponent} with the given componentId,
		 * which will make the component's events listenable to for {@link Presenter}
		 * methods annotated with @Listen.
		 * 
		 * @param <T>
		 *            The type of the {@link AbstractComponent} to register.
		 * @param componentId
		 *            The componentId to register an {@link AbstractComponent} under;
		 *            <b>not</b> allowed to be null.
		 * @param component
		 *            The component to register; <b>not</b> allowed to be null.
		 * @return The given component, for inline building
		 */
		public <T extends AbstractComponent> T registerActiveComponent(String componentId, T component) {
			if (!canRegister) {
				throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
						"The component registry may only be used during the initialization of the view it is given to, "
								+ "as components registered later would not be linked to the view's contolling subscriber anymore.");
			} else if (componentId == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a component with a null componentId.");
			} else if (component == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a null component.");
			} else {
				if (!this.activeComponents.containsKey(componentId)) {
					this.activeComponents.put(componentId, new ArrayList<>());
				}
				this.activeComponents.get(componentId).add(component);

				return component;
			}
		}

		void addListener(String componentId, Class<?> eventType, Presenter<?> presenter, Method m) {
			if (componentId == null) {
				for (List<AbstractComponent> components : this.activeComponents.values()) {
					for (AbstractComponent component : components) {
						component.addListener(eventType, presenter, m);
					}
				}
			} else if (this.activeComponents.containsKey(componentId)) {
				for (AbstractComponent component : this.activeComponents.get(componentId)) {
					component.addListener(eventType, presenter, m);
				}
			} else {
				throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR, "There is no component named '"
						+ componentId + "' registered in the view " + getClass().getSimpleName());
			}
		}
	}

	// #########################################################################################################################################
	// ################################################################# TYPE ##################################################################
	// #########################################################################################################################################

	private boolean canSetCompositionRoot = false;

	@SuppressWarnings("rawtypes")
	@Process
	private <T2 extends View, T3 extends Presenter<T2>> void initialize(TemporalInjectorCallback callback) {

		TemporalActiveComponentRegistry reg = setupUI();

		if (getClass().isAnnotationPresent(Presented.class)) {

			@SuppressWarnings("unchecked")
			Class<T3> presenterType = (Class<T3>) getClass().getAnnotation(Presented.class).value();

			Processor<T3> postProcessor = new Processor<T3>() {

				@SuppressWarnings("unchecked")
				@Override
				public void process(T3 bean, TemporalInjectorCallback callback) throws Exception {
					try {
						bean.setView((T2) View.this, reg);
					} catch (Throwable t) {
						throw new WebException(HttpErrorCodes.HTTP906_WIRING_ERROR, "The view type "
								+ getClass().getSimpleName() + " is wired to the subscriber type "
								+ bean.getClass().getSimpleName()
								+ "; setting an instance of that view on an instance of that subscriber however failed: "
								+ t.getMessage(), t);
					}
				}
			};

			callback.instantiate(TypedBlueprint.from(new TypedBlueprintTemplate<Presenter>() {

				@Override
				public Class<Presenter> getRootType() {
					return Presenter.class;
				}

				@SuppressWarnings("unchecked")
				@Define
				public BeanAllocation<Presenter> allocate() {
					BeanAllocation alloc = BeanAllocation.allocateToType(presenterType,
							PhasedProcessor.of(postProcessor, Phase.CONSTRUCT));
					return (BeanAllocation<Presenter>) alloc;
				}
			}));
		}
	}

	/**
	 * Framework internal method <b>(DO NOT USE!)</b>
	 * <P>
	 * The composition root of an {@link View} is set by the framework to the result
	 * of {@link #buildUI(TemporalActiveComponentRegistry)}; any other use of this
	 * {@link Method} is forbidden.
	 */
	@Override
	protected void setCompositionRoot(Component compositionRoot) {
		if (!this.canSetCompositionRoot) {
			throw new WebException(HttpErrorCodes.HTTP903_NOT_IMPLEMENTED_ERROR, "The composition root of an "
					+ View.class.getSimpleName() + " can only be set by the framework itself.");
		}
		super.setCompositionRoot(compositionRoot);
	}

	private TemporalActiveComponentRegistry setupUI() {
		TemporalActiveComponentRegistry reg = new TemporalActiveComponentRegistry();
		Component root = null;
		try {
			root = setupUI(reg);
		} catch (Throwable t) {
			throw new WebException(HttpErrorCodes.HTTP500_INTERNAL_SERVER_ERROR,
					"Unable to initialize view " + getClass().getSimpleName() + ": " + t.getMessage(), t);
		}
		reg.canRegister = false;
		if (root == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"The returned ui component representing the view " + getClass().getSimpleName()
							+ " was null, which is not allowed.");
		}
		this.canSetCompositionRoot = true;
		setCompositionRoot(root);
		this.canSetCompositionRoot = false;
		return reg;
	}

	Component setupUI(TemporalActiveComponentRegistry reg) throws Throwable {
		return buildUI(reg);
	}

	/**
	 * Builds this {@link View}'s UI and return it.
	 * <P>
	 * Is called automatically once after the view's injection.
	 * <P>
	 * Active components that are instantiated during the build can be registered to
	 * the given {@link TemporalActiveComponentRegistry}; they are then available to
	 * listen to by the view's {@link Presenter}'s @{@link Listen} annotated
	 * {@link Method}s.
	 * 
	 * @param reg
	 *            The {@link TemporalActiveComponentRegistry} the view may register
	 *            its active components to; may <b>not</b> be null.
	 * @return The component containing the UI that represents this view; never null
	 * @throws Throwable
	 *             For convenience, this method may throw any {@link Throwable} it
	 *             desires that can occur during its build.
	 */
	protected abstract Component buildUI(TemporalActiveComponentRegistry reg) throws Throwable;
}
