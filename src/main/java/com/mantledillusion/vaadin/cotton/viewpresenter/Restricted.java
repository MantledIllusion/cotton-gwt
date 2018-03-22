package com.mantledillusion.vaadin.cotton.viewpresenter;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.mantledillusion.injection.hura.annotation.Inspected;
import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.User;
import com.mantledillusion.vaadin.cotton.viewpresenter.View.RestrictedInspector;
import com.mantledillusion.vaadin.cotton.viewpresenter.View.RestrictedValidator;

/**
 * {@link Annotation} for {@link View} implementations that require a logged in
 * {@link User} with certain rights in order to be displayed.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Validated(RestrictedValidator.class)
@Inspected(RestrictedInspector.class)
public @interface Restricted {

	/**
	 * Defines the rightIds of the rights the {@link User} has to have upon
	 * injection of the {@link View}.
	 * <P>
	 * If no rightIds are specified the annotated {@link View} just requires a
	 * {@link User} to be logged in.
	 * 
	 * @return The rightIds the logged in user has to have to be allowed to view the
	 *         annotated {@link View}; never null, might be empty, empty by default
	 */
	String[] value() default {};
}