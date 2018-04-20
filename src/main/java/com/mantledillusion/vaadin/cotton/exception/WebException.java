package com.mantledillusion.vaadin.cotton.exception;

import java.lang.reflect.Constructor;

import org.apache.commons.lang3.StringUtils;

import com.mantledillusion.vaadin.cotton.User;
import com.mantledillusion.vaadin.cotton.environment.views.LoginView;
import com.vaadin.ui.UI;
import com.mantledillusion.vaadin.cotton.CottonServlet;
import com.mantledillusion.vaadin.cotton.QueryParam;
import com.mantledillusion.vaadin.cotton.RequiredQueryParam;
import com.mantledillusion.vaadin.cotton.UrlResourceRegistry;

/**
 * {@link RuntimeException} sub type that is used for all Cotton internal
 * {@link Exception}s; may be extended for own {@link Exception}s if desired.
 */
public class WebException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Defines HTTP error codes that can be used with a {@link WebException}.
	 * <P>
	 * Some error codes will trigger a specific reaction by the framework when
	 * caught; see a single value for details.
	 * <P>
	 * The 9XX error range is used to show errors that occurred because of misusing
	 * the framework; they should only be used internally by the framework.
	 */
	public static enum HttpErrorCodes {

		/**
		 * HTTP standard code; Used by the framework for cases when a restricted access
		 * page is entered and authenticating the user is not possible or fails.
		 * <P>
		 * If this code is thrown in a {@link WebException}, no {@link User} is logged
		 * in at that moment and a default {@link LoginView} is configured in the
		 * {@link CottonServlet}, a login + reload is attempted by the framework
		 * automatically.
		 */
		HTTP403_FORBIDDEN,

		/**
		 * HTTP standard code; Used by the framework for cases when the user navigates
		 * to an URL that has no resource registered at the {@link CottonServlet}'s
		 * {@link UrlResourceRegistry}.
		 */
		HTTP404_NOT_FOUND,

		/**
		 * HTTP standard code; Used by the framework for cases when the user navigates
		 * to an URL that causes the injection of a {@link QueryParam} annotated
		 * with @{@link RequiredQueryParam} and the parameter's specification does not
		 * match the values currently set.
		 */
		HTTP406_NOT_ACCEPTABLE,

		/**
		 * HTTP standard code; Used by the framework for cases when the user navigates
		 * to an URL that has been explicitly registered as gone at the
		 * {@link CottonServlet}'s {@link UrlResourceRegistry}.
		 */
		HTTP410_GONE,

		/**
		 * HTTP standard code; Used by the framework for general cases of failures whose
		 * reasons cannot be determined, such as wrapping occurring {@link Throwable}s
		 * of external code.
		 */
		HTTP500_INTERNAL_SERVER_ERROR,

		/**
		 * HTTP standard code; Used by the framework for cases where it detects a loop
		 * of some kind that would lead to {@link StackOverflowError}s.
		 */
		HTTP508_LOOP_DETECTED,

		/**
		 * Framework error code; Error that may occur when trying to statically execute
		 * functions on the current {@link UI} when there is none.
		 */
		HTTP900_UI_INACTIVITY_ERROR,

		/**
		 * Framework error code; Error that may occur when calling a function that is
		 * part of the framework, but the given arguments do not fit the requirements;
		 * equals {@link IllegalArgumentException}.
		 */
		HTTP901_ILLEGAL_ARGUMENT_ERROR,

		/**
		 * Framework error code; Error that may occur when calling a function that is
		 * part of the framework, but the framework is in a current state that does not
		 * allow that function; equals {@link IllegalStateException}.
		 */
		HTTP902_ILLEGAL_STATE_ERROR,

		/**
		 * Framework error code; Error that may occur when calling a function that is
		 * part of the framework, but is not implemented (yet or out of purpose) so it
		 * may not be used.
		 */
		HTTP903_NOT_IMPLEMENTED_ERROR,

		/**
		 * Framework error code; Error that may occur when a framework annotation is
		 * used in a place or way it is not destined for.
		 */
		HTTP904_ILLEGAL_ANNOTATION_USE,

		/**
		 * Framework error code; Error that may occur when a framework interface is used
		 * in a place or way it is not destined for.
		 */
		HTTP905_ILLEGAL_INTERFACE_USE,

		/**
		 * Error that may occur when a type has to be wired that cannot be
		 * instantiated/... for some reason.
		 */
		HTTP906_WIRING_ERROR,

		/**
		 * Framework error code; Error that may occur when a structure given to the
		 * framework has some major flaws that do not match the framework's requirements
		 * for such a structure.
		 */
		HTTP907_ILLEGAL_STRUCTURING;

		private final short code;
		private final String name;

		private HttpErrorCodes() {
			this.code = Short.valueOf(name().substring(4, name().indexOf('_')));
			this.name = StringUtils.replace(name().substring(name().indexOf('_') + 1), "_", StringUtils.SPACE);
		}

		/**
		 * Returns the HTTP error code as a numerical value.
		 * 
		 * @return The HTTP error code; never null
		 */
		public short getCode() {
			return code;
		}

		/**
		 * Returns the error name of this HTTP error.
		 * 
		 * @return A {@link String} representation of the error; never null
		 */
		public String getName() {
			return name;
		}
	}

	private final HttpErrorCodes errorCode;

	/**
	 * {@link Constructor} for {@link WebException}s without cause.
	 * 
	 * @param errorCode
	 *            The error code; may NOT be null.
	 * @param message
	 *            The message in addition to the error code and its name; might be
	 *            null.
	 */
	public WebException(HttpErrorCodes errorCode, String message) {
		this(errorCode, message, null);
	}

	/**
	 * {@link Constructor} for {@link WebException}s with cause.
	 * 
	 * @param errorCode
	 *            The error code; may NOT be null.
	 * @param message
	 *            The message in addition to the error code and its name; might be
	 *            null.
	 * @param cause
	 *            The cause that this {@link WebException} occurred; might be null.
	 */
	public WebException(HttpErrorCodes errorCode, String message, Throwable cause) {
		super(message, cause);
		if (errorCode == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Unable to create "
					+ WebException.class.getSimpleName() + " with a null " + HttpErrorCodes.class.getSimpleName());
		}
		this.errorCode = errorCode;
	}

	/**
	 * Returns the error code of this {@link WebException}.
	 * 
	 * @return The error code in enumerated form; never null
	 */
	public HttpErrorCodes getErrorCode() {
		return errorCode;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (http " + errorCode.code + " - " + errorCode.name + "): "
				+ super.toString();
	}
}
