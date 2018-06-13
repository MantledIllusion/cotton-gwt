package com.mantledillusion.vaadin.cotton.viewpresenter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.mantledillusion.injection.hura.AnnotationValidator;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.vaadin.cotton.EventBusSubscriber;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.View.TemporalActiveComponentRegistry;
import com.vaadin.ui.AbstractComponent;

/**
 * Basic super type for a presenter that controls an {@link View}.
 * <p>
 * NOTE: Should be injected, since the {@link Injector} handles the instance's
 * life cycles.
 * <P>
 * Instances of sub types of {@link Presenter} will be instantiated
 * automatically during injection for every {@link View} implementation that
 * requires controlling by an @{@link Presented} annotation on that view type.
 * <P>
 * The {@link Presenter} will automatically be connected to the view it belongs
 * to, that view can be retrieved by calling {@link #getView()}.
 * <P>
 * All {@link Method}s of this {@link Presenter} implementation that are
 * annotated with @{@link Listen} will receive specifiable events of
 * {@link AbstractComponent}s on the connected view that have been registered as
 * active component to the {@link TemporalActiveComponentRegistry} during the
 * view's UI build; see the documentation of @{@link Listen} for details.
 * <P>
 * Since {@link Presenter} extends {@link EventBusSubscriber}, all
 * {@link Method}s of an {@link Presenter} implementation that are annotated
 * with @{@link Subscribe} will receive specifiable events of other
 * {@link EventBusSubscriber}s; see the documentation of @{@link Subscribe} for
 * reference.
 *
 * @param <T>
 *            The type of {@link View} this {@link Presenter} can control.
 */
public abstract class Presenter<T extends View> extends EventBusSubscriber {

	// #########################################################################################################################################
	// ################################################################ LISTEN #################################################################
	// #########################################################################################################################################

	static class ListenValidator implements AnnotationValidator<Listen, Method> {

		@Override
		public void validate(Listen annotationInstance, Method annotatedElement) throws Exception {
			Class<?> listeningType = annotatedElement.getDeclaringClass();

			if (!Presenter.class.isAssignableFrom(listeningType)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The @" + Listen.class.getSimpleName() + " annotation can only be used on "
								+ Presenter.class.getSimpleName() + " implementations; the type '"
								+ listeningType.getSimpleName() + "' however is not.");
			} else if (Modifier.isStatic(annotatedElement.getModifiers())) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The method '" + annotatedElement.getName() + "' of the type '" + listeningType.getSimpleName()
								+ "' annotated with @" + Listen.class.getSimpleName()
								+ " is static, which is not allowed.");
			} else if (annotatedElement.getParameterCount() > 1) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"Methods annotated with @" + Listen.class.getSimpleName()
								+ " are only allowed to have 0 or 1 parameters. The method "
								+ annotatedElement.getName() + " of the type '" + listeningType.getSimpleName()
								+ "' however has " + annotatedElement.getParameterCount());
			}
		}
	}

	// #########################################################################################################################################
	// ############################################################### SUBSCRIBE ###############################################################
	// #########################################################################################################################################

	static class SubscribeValidator implements AnnotationValidator<Subscribe, Method> {

		@Override
		public void validate(Subscribe annotationInstance, Method annotatedElement) throws Exception {
			Class<?> subscribingType = annotatedElement.getDeclaringClass();

			if (!EventBusSubscriber.class.isAssignableFrom(subscribingType)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The @" + Subscribe.class.getSimpleName() + " annotation can only be used on "
								+ EventBusSubscriber.class.getSimpleName() + " implementations; the type '"
								+ subscribingType.getSimpleName() + "' however is not.");
			} else if (Modifier.isStatic(annotatedElement.getModifiers())) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The method '" + annotatedElement.getName() + "' of the type '"
								+ subscribingType.getSimpleName() + "' annotated with @"
								+ Subscribe.class.getSimpleName() + " is static, which is not allowed.");
			} else if (annotatedElement.getParameterCount() == 0 && annotationInstance.anonymousEvents().length == 0) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE, "Methods annotated with "
						+ Subscribe.class.getSimpleName()
						+ " are only allowed to have no parameter if there is at least one anonymous event type set; the method '"
						+ annotatedElement.getName() + "' of the type '" + subscribingType.getSimpleName()
						+ "' however has 0 of both.F");
			} else if (annotatedElement.getParameterCount() > 1) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"Methods annotated with " + Subscribe.class.getSimpleName()
								+ " are only allowed to have a maximum of 1 parameter; the method '"
								+ annotatedElement.getName() + "' of the type '" + subscribingType.getSimpleName()
								+ "' however has " + annotatedElement.getParameterCount());
			}

			if (annotatedElement.getParameterCount() > 0) {
				Class<?> eventType = annotatedElement.getParameterTypes()[0];

				if (!BusEvent.class.isAssignableFrom(eventType)) {
					throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
							"Methods annotated with " + Subscribe.class.getSimpleName()
									+ " are only allowed to have 1 parameter that is a sub type of "
									+ BusEvent.class.getSimpleName() + "; the method '" + annotatedElement.getName()
									+ "' of the type '" + subscribingType.getSimpleName()
									+ "' however has 1 parameter of the type '" + eventType.getSimpleName()
									+ "' which is not.");
				}
			}
		}
	}

	// #########################################################################################################################################
	// ################################################################## TYPE #################################################################
	// #########################################################################################################################################

	private T view;

	protected final T getView() {
		return view;
	}

	final void setView(T view, TemporalActiveComponentRegistry reg) {
		this.view = view;

		for (Method method : MethodUtils.getMethodsListWithAnnotation(getClass(), Listen.class, true, true)) {
			// COMPONENT EVENT METHODS
			if (method.isAnnotationPresent(Listen.class)) {
				if (!method.isAccessible()) {
					try {
						method.setAccessible(true);
					} catch (SecurityException e) {
						throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
								"Unable to gain access to the method '" + method.getName() + "' of the type "
										+ Presenter.this.getClass().getSimpleName() + ".",
								e);
					}
				}

				Class<?> eventType = null;
				if (method.getParameterCount() == 1) {
					eventType = method.getParameterTypes()[0];
				} else {
					eventType = Object.class;
				}

				Listen annotation = method.getAnnotation(Listen.class);

				if (annotation.value().length == 0) {
					reg.addListener(null, eventType, this, method);
				} else {
					for (Listen.ActiveComponent activeComp : annotation.value()) {
						reg.addListener(activeComp.value(), eventType, this, method);
					}
				}
			}
		}
	}
}
