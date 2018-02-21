package com.mantledillusion.vaadin.cotton.viewpresenter;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter.ListenValidator;
import com.vaadin.ui.AbstractComponent;

/**
 * {@link Annotation} for {@link Method}s of {@link Presenter}
 * implementations that need to listen to events of {@link Listen.ActiveComponent}s of
 * the {@link Presenter}'s {@link View}.
 * <P>
 * An annotated method is expected to be a void method that has to receive
 * either 0 or 1 parameter:<br>
 * - If it has 0 parameters, the method will be called for every event a
 * declared {@link Listen.ActiveComponent} fires. <br>
 * - If it has 1 parameter, the method will only be called for events of that
 * type or that type's sub types.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Validated(ListenValidator.class)
public @interface Listen {

	/**
	 * Defines a single {@link AbstractComponent} registered as active component on
	 * the {@link View} of the {@link Presenter} the @{@link Listen}
	 * annotated {@link Method} is defined on.
	 */
	public @interface ActiveComponent {
	
		/**
		 * Defines the componentId whose corresponding {@link AbstractComponent} the
		 * {@link Method} annotated with @{@link Listen} listens to.
		 * <P>
		 * The componentId has to be the one that is used upon registering the
		 * {@link AbstractComponent} as an active component to its {@link View}
		 * during the view's UI setup.
		 * 
		 * @return The componentId of the corresponding {@link AbstractComponent}; never
		 *         null
		 */
		String value();
	}

	/**
	 * The {@link Listen.ActiveComponent}s the annotated {@link Method} listens for.
	 * <P>
	 * By default none are declared, so the {@link Method} will listen to <B>all</B>
	 * active components the {@link Presenter}'s {@link View}
	 * registers.
	 * 
	 * @return The {@link Listen.ActiveComponent}s to listen to; never null, might be empty
	 */
	ActiveComponent[] value() default {};
}