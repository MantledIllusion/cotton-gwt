package com.mantledillusion.vaadin.cotton.viewpresenter;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter.ListenValidator;
import com.vaadin.ui.AbstractComponent;

/**
 * {@link Annotation} for {@link Method}s of {@link Presenter} implementations
 * that need to listen to events of active {@link AbstractComponent}s of the
 * {@link Presenter}'s {@link View}.
 * <P>
 * An annotated {@link Method} is expected to be a void {@link Method} and has
 * to receive exactly 1 {@link Parameter} of an event implementation,
 * or have at least one anonymous event type set.
 * <P>
 * The {@link Method} will be called for events of that {@link Parameter}'s type
 * and every sub type. Alternatively or additionally, it will be called for the
 * anonymous event types set in this annotation.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Validated(ListenValidator.class)
public @interface Listen {

	/**
	 * Defines the componentId whose corresponding {@link AbstractComponent} the
	 * {@link Method} annotated with @{@link Listen} listens to.
	 * <P>
	 * The componentId has to be the one that is used upon registering the
	 * {@link AbstractComponent} as an active component to its {@link View} during
	 * the view's UI setup.
	 * <P>
	 * By default none are declared, so the {@link Method} will listen to <B>all</B>
	 * active components the {@link Presenter}'s {@link View} registers.
	 * 
	 * @return The {@link AbstractComponent}s to listen to; never null, might be
	 *         empty
	 */
	String[] value() default {};

	/**
	 * Determines the anonymous events to trigger the annotated method with as well,
	 * just without passing the event instance to the method as a parameter.
	 * <p>
	 * A method annotated with @{@link Listen} might be parameterless if at least
	 * one anonymous event is set.
	 * 
	 * @return The event types that might trigger the annotated method without being
	 *         a parameter; never null, empty by default
	 */
	Class<?>[] anonymousEvents() default {};
}