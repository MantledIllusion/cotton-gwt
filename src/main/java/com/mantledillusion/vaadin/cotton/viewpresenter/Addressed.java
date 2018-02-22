package com.mantledillusion.vaadin.cotton.viewpresenter;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.UrlResourceRegistry;
import com.mantledillusion.vaadin.cotton.viewpresenter.View.AddressableValidator;

/**
 * {@link Annotation} for {@link View} implementations that get manually
 * registered on an {@link UrlResourceRegistry} instance as the main view to be
 * available under a given URL path.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Validated(AddressableValidator.class)
public @interface Addressed {

	/**
	 * Defines a segmented URL path whose requests will be redirected.
	 */
	public @interface Redirect {

		/**
		 * The segmented URL path that will be redirected.
		 * <P>
		 * The URL has to be validateable by
		 * {@link UrlResourceRegistry#checkUrlPattern(String)}.
		 * 
		 * @return The URL that is redirected; never null
		 */
		String value();
	}

	/**
	 * The segmented URL path the annotated {@link View} has to be addressable by.
	 * <P>
	 * The URL has to match the {@link Pattern}
	 * {@link UrlResourceRegistry#URL_PATH_REGEX}.
	 * 
	 * @return The URL the annotated {@link View} has to be addressable by; never
	 *         null
	 */
	String value();

	/**
	 * The segmented URL paths that will be redirected to the annotated
	 * {@link View}.
	 * 
	 * @return The URLs whose requests will be redirected to {@link #value()}, never
	 *         null, might be empty for no redirects
	 */
	Redirect[] redirects() default {};
}