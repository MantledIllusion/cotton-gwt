package com.mantledillusion.vaadin.cotton.model;

import java.lang.reflect.Method;

import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.AbstractComponent;

/**
 * Error for {@link AbstractComponent}s.
 */
public final class ValidationError extends AbstractErrorMessage {

	private static final long serialVersionUID = 1L;

	/**
	 * Defines the possible levels of validity a validation might have.
	 */
	public static enum ValidityLevel {

		/**
		 * The validation is fully valid.
		 */
		VALID(true),

		/**
		 * The validation is valid, although one or more warnings were found.
		 */
		WARNING(true),

		/**
		 * The validation is invalid.
		 */
		ERROR(false);

		private final boolean isValid;

		private ValidityLevel(boolean isValid) {
			this.isValid = isValid;
		}

		public boolean isValid() {
			return isValid;
		}
	}

	private ValidationError(String message, ErrorLevel level) {
		super(message);
		setMode(ContentMode.HTML);
		setErrorLevel(level);
	}

	@Override
	public int hashCode() {
		String message = getMessage();
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		String message = getMessage();
		String other = ((ValidationError) obj).getMessage();
		if (message == null) {
			if (other != null)
				return false;
		} else if (!message.equals(other))
			return false;
		return true;
	}

	/**
	 * Factory {@link Method} for {@link ValidationError}s.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * <p>
	 * {@link ErrorLevel#ERROR} is used as the default error level.
	 * <p>
	 * Note that errors with an error level ordinal lower than
	 * {@link ErrorLevel#ERROR} (such as {@link ErrorLevel#INFO} or
	 * {@link ErrorLevel#WARNING}) will be displayed on the validated field, but
	 * will not not cause the validation to fail.
	 * 
	 * @param errorMsgId
	 *            The error message (or localizable error message id); might
	 *            <b>not</b> be null.
	 * @return A new {@link ValidationError}; never null
	 */
	public static ValidationError of(String errorMsgId) {
		return of(errorMsgId, ErrorLevel.ERROR);
	}

	/**
	 * Factory {@link Method} for {@link ValidationError}s.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * <p>
	 * Note that errors with an error level ordinal lower than
	 * {@link ErrorLevel#ERROR} (such as {@link ErrorLevel#INFO} or
	 * {@link ErrorLevel#WARNING}) will be displayed on the validated field, but
	 * will not not cause the validation to fail.
	 * 
	 * @param errorMsgId
	 *            The error message (or localizable error message id); might
	 *            <b>not</b> be null.
	 * @param errorLevel
	 *            The error level; might <b>not</b> be null.
	 * @return A new {@link ValidationError}; never null
	 */
	public static ValidationError of(String errorMsgId, ErrorLevel errorLevel) {
		if (errorMsgId == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Unable to create null-message error");
		} else if (errorLevel == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Unable to create an error with a null level");
		}
		return new ValidationError(WebEnv.localize(errorMsgId), errorLevel);
	}
}