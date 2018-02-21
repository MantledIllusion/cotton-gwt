package com.mantledillusion.vaadin.cotton;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import com.mantledillusion.injection.hura.annotation.Inspected;
import com.mantledillusion.injection.hura.annotation.Validated;
import com.mantledillusion.vaadin.cotton.Cookie.RequiredCookieInspector;
import com.mantledillusion.vaadin.cotton.Cookie.RequiredCookieValidator;

/**
 * {@link Annotation} for {@link Cookie} type fields that require a cookie of a
 * specifiable name.
 */
@Retention(RUNTIME)
@Target(FIELD)
@Validated(RequiredCookieValidator.class)
@Inspected(RequiredCookieInspector.class)
public @interface RequiredCookie {

	/**
	 * The {@link Cookie} name that identifies the cookie.
	 * 
	 * @return The cookie's name; has to match {@link Cookie#COOKIE_REGEX}
	 */
	String value();

	/**
	 * The time period length this cookie has to be valid until it expires; the
	 * period's unit might be defined by {@link #unit()}.
	 * <p>
	 * The period is measured from the moment the {@link RequiredCookie}'s parent
	 * bean is destroyed.
	 * <p>
	 * Setting this to a value < 0 defines this cookie as a session cookie that
	 * lasts until the session is closed.
	 * 
	 * @return The period in relation to the unit until the annotated cookie
	 *         expires; -1 by default
	 */
	long period() default -1;

	/**
	 * The unit of the period length specified by {@link #period()}.
	 * 
	 * @return The unit of the expiring period; {@link ChronoUnit#SECONDS} by
	 *         default
	 */
	ChronoUnit unit() default ChronoUnit.SECONDS;

	/**
	 * Returns whether the {@link Cookie} always has to be set.
	 * <p>
	 * If set to true, the {@link Annotation} will cause the default value to be
	 * automatically set.
	 * 
	 * @return True if the {@link Cookie} always has to be set, false otherwise;
	 *         false by default
	 */
	boolean forced() default false;

	/**
	 * The default value to use when the cookie does not exist.
	 * <p>
	 * This value is only applied if {@link #forced()} is true.
	 * 
	 * @return The default value to use; never null, empty by default
	 */
	String defaultValue() default "";
}