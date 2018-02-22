package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import com.mantledillusion.injection.hura.AnnotationValidator;
import com.mantledillusion.injection.hura.Inspector;
import com.mantledillusion.injection.hura.Injector.TemporalInjectorCallback;
import com.mantledillusion.injection.hura.annotation.Construct;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

/**
 * Representation type of a {@link Cookie}.
 */
public final class Cookie {

	static final String COOKIE_REGEX = "[^=;,\\s]+";

	// #########################################################################################################################################
	// ########################################################### REQUIRED  COOKIE ############################################################
	// #########################################################################################################################################

	static class RequiredCookieValidator implements AnnotationValidator<RequiredCookie, Field> {

		@Override
		public void validate(RequiredCookie annotationInstance, Field annotatedElement) throws Exception {
			if (!TypeUtils.isAssignable(Cookie.class, annotatedElement.getGenericType())) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The field '" + annotatedElement.getName() + "' in the type '"
								+ annotatedElement.getDeclaringClass().getSimpleName() + "' is annotated with @"
								+ RequiredCookie.class.getSimpleName()
								+ ", but the fields type is not assignable by an instance of "
								+ Cookie.class.getSimpleName());
			} else if (Modifier.isStatic(annotatedElement.getModifiers())) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The field '" + annotatedElement.getName() + "' in the type '"
								+ annotatedElement.getDeclaringClass().getSimpleName() + "' is annotated with @"
								+ RequiredCookie.class.getSimpleName() + ", but the field is static.");
			} else if (!annotationInstance.value().matches(COOKIE_REGEX)) {
				throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
						"The field '" + annotatedElement.getName() + "' in the type '"
								+ annotatedElement.getDeclaringClass().getSimpleName() + "' is annotated with @"
								+ RequiredCookie.class.getSimpleName() + ", but the cookie's name '"
								+ annotationInstance.value() + "' does not match the pattern '" + COOKIE_REGEX
								+ "' for cookie names.");
			}
		}
	}

	static class RequiredCookieInspector implements Inspector<RequiredCookie, Field> {

		@Construct
		private RequiredCookieInspector() {
		}

		@Override
		public void inspect(Object bean, RequiredCookie annotationInstance, Field annotatedElement,
				TemporalInjectorCallback callback) throws Exception {
			Cookie cookie = new Cookie(annotationInstance.value(), annotationInstance.period(), annotationInstance.unit());
			
			annotatedElement.setAccessible(true);
			annotatedElement.set(bean, cookie);
			
			if (annotationInstance.forced() && !cookie.exists()) {
				cookie.setValue(annotationInstance.defaultValue());
			}
		}
	}

	// #########################################################################################################################################
	// ################################################################# TYPE ##################################################################
	// #########################################################################################################################################

	private final String name;
	private final long period;
	private final ChronoUnit unit;

	private Cookie(String name, long period, ChronoUnit unit) {
		this.name = name;
		this.period = period;
		this.unit = unit;
	}

	/**
	 * Returns whether there is an unexpired {@link Cookie} on the client.
	 * 
	 * @return True if there is an unexpired {@link Cookie}; false otherwise
	 */
	public boolean exists() {
		return CottonUI.current().hasCookie(this.name);
	}

	/**
	 * Returns the name of the {@link Cookie}.
	 * 
	 * @return The name; never null
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the value of the {@link Cookie}.
	 * 
	 * @return The value; never null
	 */
	public String getValue() {
		return CottonUI.current().getCookie(this.name);
	}

	/**
	 * Sets the given value as the value of this {@link Cookie}.
	 * 
	 * @param value
	 *            The values to set; might <b>not</b> be null
	 */
	public void setValue(String value) {
		if (value == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot set the cookie value to null.");
		}
		CottonUI.current().setCookie(this.name, value, getExpiringDate());
	}

	private ZonedDateTime getExpiringDate() {
		return period < 0 ? null : ZonedDateTime.now().plus(this.period, this.unit);
	}

	@Override
	public String toString() {
		return this.name + '=' + StringUtils.join(getValue(), ',');
	}
}
