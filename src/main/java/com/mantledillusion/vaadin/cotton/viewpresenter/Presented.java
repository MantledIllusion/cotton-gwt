package com.mantledillusion.vaadin.cotton.viewpresenter;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.viewpresenter.View.PresentValidator;

/**
 * {@link Annotation} for {@link View} implementations that need controlling by
 * an {@link Presenter} implementation.
 * <P>
 * {@link Presenter}s for {@link View}s are instantiated to be completely
 * autonomous, without any possibility to be injected elsewhere.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Validated(PresentValidator.class)
public @interface Presented {

	/**
	 * Defines the {@link Presenter}'s implementation type that will be instantiated
	 * for instances of the annotated {@link View}.
	 *
	 * @return The {@link Presenter} implementation that presents instances of this
	 *         {@link View} implementation; never null
	 */
	Class<? extends Presenter<? extends View>> value();
}