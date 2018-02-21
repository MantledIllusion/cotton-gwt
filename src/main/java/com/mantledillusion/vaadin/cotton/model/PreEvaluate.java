package com.mantledillusion.vaadin.cotton.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.model.ValidationContext.PreEvaluateValidator;

/**
 * {@link Annotation} for {@link Validator}s that points at one or more other
 * {@link Validator}s whose evaluation results are {@link PreEvaluate.Prerequisite}s for
 * the annotated {@link Validator} to be evaluated itself.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Validated(PreEvaluateValidator.class)
public @interface PreEvaluate {
	
	/**
	 * Defines a {@link Prerequisite} of a {@link Validator}.
	 * <P>
	 * This {@link Prerequisite} will be evaluated before the {@link Validator}
	 * annotated with @{@link PreEvaluate}; the {@link Prerequisite}'s result will
	 * determine whether the annotated {@link Validator} is even executed.
	 */
	public @interface Prerequisite {
	
		/**
		 * The {@link Prerequisite}s class type.
		 * 
		 * @return The {@link Class} of the {@link Validator} implementation to
		 *         instantiate and execute beforehand; never null
		 */
		Class<? extends Validator<?>> value();
	
		/**
		 * Determines whether this {@link Prerequisite}'s validation result has to be
		 * true (no errors) or false (one or more validation errors) in order for the
		 * {@link Validator} annotated with @{@link PreEvaluate} to be evaluated.
		 * 
		 * @return True if the {@link Prerequisite} has to evaluate valid, false
		 *         otherwise; true by default
		 */
		boolean requiredResult() default true;
	}

	/**
	 * The {@link PreEvaluate.Prerequisite} {@link Validator}s the annotated {@link Validator}
	 * requires to be evaluated beforehand.
	 * 
	 * @return the {@link PreEvaluate.Prerequisite}s to this {@link Validator}; never null, might be empty
	 */
	Prerequisite[] value();
}