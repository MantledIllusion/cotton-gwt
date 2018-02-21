package com.mantledillusion.vaadin.cotton;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

import com.mantledillusion.injection.hura.annotation.Inspected;
import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.CottonUI.NavigationEvent;
import com.mantledillusion.vaadin.cotton.QueryParam.RequiredQueryParamInspector;
import com.mantledillusion.vaadin.cotton.QueryParam.RequiredQueryParamValidator;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

/**
 * {@link Annotation} for {@link QueryParam} typed fields that require a query
 * parameter of a specifiable key.
 * <p>
 * As long as a bean of the annotated field is not destroyed, the annotation
 * will also monitor the changes on the {@link QueryParam} using
 * {@link NavigationEvent}.
 */
@Retention(RUNTIME)
@Target(FIELD)
@Validated(RequiredQueryParamValidator.class)
@Inspected(RequiredQueryParamInspector.class)
public @interface RequiredQueryParam {

	/**
	 * The {@link QueryParam} key that identifies the required param.
	 * 
	 * @return The required param's key; not allowed to be blank
	 */
	String value();

	/**
	 * Returns the exact amount of values for this {@link QueryParam} that need to
	 * be set.
	 * <p>
	 * If the count does not set the returned amount, a
	 * {@link HttpErrorCodes#HTTP406_NOT_ACCEPTABLE} {@link WebException} is thrown.
	 * 
	 * @return The value count of the {@link QueryParam}; 0 is not allowed, negative
	 *         values mean 'not required to be a specific count', -1 by default
	 */
	int valueCount() default -1;

	/**
	 * The {@link Pattern} matcher for each and every {@link QueryParam} value to
	 * match.
	 * 
	 * @return The matcher for the {@link QueryParam}s values; never null, must be
	 *         parsable by {@link Pattern#compile(String)}, '.*' by default to match
	 *         every possible param value
	 */
	String matcher() default ".*";

	/**
	 * Returns whether the {@link QueryParam} always has to be set.
	 * <p>
	 * If set to true, the {@link Annotation} will cause the default values to be
	 * automatically set. If there are none, a
	 * {@link HttpErrorCodes#HTTP406_NOT_ACCEPTABLE} {@link WebException} is thrown.
	 * 
	 * @return True if the {@link QueryParam} always has to be set, false otherwise;
	 *         false by default
	 */
	boolean forced() default false;

	/**
	 * The default values to use when the required param is not set.
	 * <p>
	 * These values are only applied if {@link #forced()} is true.
	 * 
	 * @return The default params to use; never null, an empty array is considered
	 *         as 'do not use default values', not allowed to contain blank values,
	 *         empty by default
	 */
	String[] defaultValues() default {};
}