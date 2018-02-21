package com.mantledillusion.vaadin.cotton.viewpresenter;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.EventBusSubscriber;
import com.mantledillusion.vaadin.cotton.EventBusSubscriber.BusEvent;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter.SubscribeValidator;

/**
 * {@link Annotation} for {@link Method}s of {@link EventBusSubscriber}
 * implementations that subscribe to the global
 * {@link EventBusSubscriber}'s event bus.
 * <P>
 * An annotated {@link Method} is expected to be a void and has to receive
 * exactly 1 {@link Parameter} of an {@link BusEvent} implementation.
 * <P>
 * The {@link Method} will be called for events of that {@link Parameter}'s type
 * and every sub type.
 * <P>
 * The events may be automatically filtered using {@link Subscribe.EventProperty}s.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Validated(SubscribeValidator.class)
public @interface Subscribe {

	/**
	 * Defines a property of an {@link BusEvent}, as it may occur in an
	 * instance of an {@link BusEvent} implementation instance when it is
	 * passed to the {@link Method} annotated with @{@link Subscribe} by the event
	 * bus.
	 */
	public @interface EventProperty {
	
		/**
		 * The key of the event property.
		 * 
		 * @return The property key; never null
		 */
		String key();
	
		/**
		 * The value of the event property.
		 * 
		 * @return The property value; never null
		 */
		String value();
	}

	/**
	 * The {@link Subscribe.EventProperty}s a type-matching event <b>all</b> has to have to
	 * trigger the subscribed {@link Method}.
	 * <p>
	 * This is a convenience mechanism for pre-filtering, preventing having to
	 * repeatedly ask an incoming {@link BusEvent} instance in a
	 * {@link Method} annotated with {@link Subscribe} if its properties equal some
	 * specific values.
	 * 
	 * @return The required {@link Subscribe.EventProperty}s; never null, empty by default
	 */
	EventProperty[] value() default {};

	/**
	 * Determines whether the annotated {@link Method} of an
	 * {@link EventBusSubscriber} may receive {@link BusEvent}s that
	 * the same {@link EventBusSubscriber} instance has dispatched.
	 * 
	 * @return True if the {@link Method} can retrieve events of its own
	 *         {@link EventBusSubscriber} instance, false otherwise; true by
	 *         default
	 */
	boolean isSelfObservant() default true;
}