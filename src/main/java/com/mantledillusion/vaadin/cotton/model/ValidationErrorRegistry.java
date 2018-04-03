package com.mantledillusion.vaadin.cotton.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.model.ValidationError.ValidityLevel;
import com.vaadin.shared.ui.ErrorLevel;

/**
 * An error registry to register found validation errors at.
 *
 * @param <ModelType>
 *            The root type of the data model the
 *            {@link ValidationErrorRegistry} can hold validation errors of.
 */
public final class ValidationErrorRegistry<ModelType> {

	private final Map<ModelProperty<ModelType, ?>, Set<ValidationError>> errorMessages = new IdentityHashMap<>();

	ValidationErrorRegistry() {
	}

	/**
	 * Adds the given error message to the registry for all the given
	 * {@link ModelProperty}s.
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
	 *            The error message (or localizable error message id) to use;
	 *            <b>not</b> allowed to be null.
	 * @param property
	 *            The first {@link ModelProperty} to register the error for;
	 *            <b>not</b> allowed to be null.
	 * @param additionalProperties
	 *            The second -&gt; nth {@link ModelProperty} to register the error
	 *            for; might be null or contain null.
	 */
	@SafeVarargs
	public final void addError(String errorMsgId, ModelProperty<ModelType, ?> property,
			ModelProperty<ModelType, ?>... additionalProperties) {
		addError(ValidationError.of(errorMsgId), property, additionalProperties);
	}

	/**
	 * Adds the given error message to the registry for all the given
	 * {@link ModelProperty}s.
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
	 *            The error message (or localizable error message id) to use; might
	 *            <b>not</b> be null.
	 * @param errorLevel
	 *            The error level of the error; might <b>not</b> be null.
	 * @param property
	 *            The first {@link ModelProperty} to register the error for; might
	 *            <b>not</b> be null.
	 * @param additionalProperties
	 *            The second -&gt; nth {@link ModelProperty} to register the error
	 *            for; might be null or contain null.
	 */
	@SafeVarargs
	public final void addError(String errorMsgId, ErrorLevel errorLevel, ModelProperty<ModelType, ?> property,
			ModelProperty<ModelType, ?>... additionalProperties) {
		addError(ValidationError.of(errorMsgId, errorLevel), property, additionalProperties);
	}

	/**
	 * Adds the given error message to the registry for all the given
	 * {@link ModelProperty}s.
	 * <p>
	 * Note that errors with an error level ordinal lower than
	 * {@link ErrorLevel#ERROR} (such as {@link ErrorLevel#INFO} or
	 * {@link ErrorLevel#WARNING}) will be displayed on the validated field, but
	 * will not not cause the validation to fail.
	 * 
	 * @param error
	 *            The error to add; <b>not</b> allowed to be null.
	 * @param property
	 *            The first {@link ModelProperty} to register the error for;
	 *            <b>not</b> allowed to be null.
	 * @param additionalProperties
	 *            The second -&gt; nth {@link ModelProperty} to register the error
	 *            for; might be null or contain null.
	 */
	@SafeVarargs
	public final void addError(ValidationError error, ModelProperty<ModelType, ?> property,
			ModelProperty<ModelType, ?>... additionalProperties) {
		if (property == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Unable to add an error for a null property.");
		} else if (error == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Unable to add a null error for a property.");
		}
		add(property, error);
		if (additionalProperties != null) {
			for (ModelProperty<ModelType, ?> prop : additionalProperties) {
				if (prop != null) {
					add(prop, error);
				}
			}
		}
	}

	private void add(ModelProperty<ModelType, ?> property, ValidationError error) {
		if (!this.errorMessages.containsKey(property)) {
			this.errorMessages.put(property, new HashSet<>());
		}
		this.errorMessages.get(property).add(error);
	}

	void addAll(ValidationErrorRegistry<ModelType> other) {
		for (ModelProperty<ModelType, ?> property : other.errorMessages.keySet()) {
			if (this.errorMessages.containsKey(property)) {
				this.errorMessages.get(property).addAll(other.errorMessages.get(property));
			} else {
				this.errorMessages.put(property, new HashSet<>(other.errorMessages.get(property)));
			}
		}
	}

	/**
	 * Determines whether this registries' validity.
	 * 
	 * @return The level of validity; never null
	 */
	public ValidityLevel getValidity() {
		ValidityLevel result = ValidityLevel.VALID;
		for (Set<ValidationError> propertyErrors : this.errorMessages.values()) {
			switch (containsError(propertyErrors)) {
			case ERROR:
				return ValidityLevel.ERROR;
			case WARNING:
				result = ValidityLevel.WARNING;
			default:
				break;
			}
		}
		return result;
	}

	static ValidityLevel containsError(Set<ValidationError> errors) {
		ValidityLevel result = ValidityLevel.VALID;
		for (ValidationError error : errors) {
			if (error.getErrorLevel().ordinal() >= ErrorLevel.ERROR.ordinal()) {
				return ValidityLevel.ERROR;
			} else {
				result = ValidityLevel.WARNING;
			}
		}
		return result;
	}

	/**
	 * Returns whether there is one or more {@link ValidationError}s for the given
	 * property.
	 * 
	 * @param property
	 *            The {@link ModelProperty} to check; might be null, although the
	 *            result will always be false
	 * @return True if there is one or more errors for the given property, false
	 *         otherwise
	 */
	public boolean hasErrorsForProperty(ModelProperty<ModelType, ?> property) {
		return this.errorMessages.containsKey(property);
	}

	/**
	 * Returns the {@link Set} of {@link ValidationError}s of the given property.
	 * 
	 * @param property
	 *            The {@link ModelProperty} to get the errors of; might be null,
	 *            although the result will always be an empty {@link Set}
	 * @return The errors of the given property; never null, might be empty if there
	 *         are none
	 */
	public Set<ValidationError> getErrorsOfProperty(ModelProperty<ModelType, ?> property) {
		if (this.errorMessages.containsKey(property)) {
			return Collections.unmodifiableSet(this.errorMessages.get(property));
		} else {
			return Collections.emptySet();
		}
	}
}