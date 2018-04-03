package com.mantledillusion.vaadin.cotton.model;

import java.util.Collection;

import com.vaadin.shared.ui.ErrorLevel;

/**
 * Interface for validators that validate raw field input.
 *
 * @param <FieldValueType>
 *            The type of the raw field input.
 */
public interface InputValidator<FieldValueType> {

	/**
	 * Returns 0-&gt;n {@link ValidationError}s for the given input.
	 * <p>
	 * Note that errors with an error level ordinal lower than
	 * {@link ErrorLevel#ERROR} (such as {@link ErrorLevel#INFO} or
	 * {@link ErrorLevel#WARNING}) will be displayed on the validated field, but
	 * will not not cause the validation to fail.
	 * 
	 * @param value
	 *            The input value to validate; might be null if the field's empty
	 *            representation is null.
	 * @return The errors found in the given input; might be null or contain nulls,
	 *         both is ignored
	 */
	Collection<ValidationError> validateInput(FieldValueType value);
}
