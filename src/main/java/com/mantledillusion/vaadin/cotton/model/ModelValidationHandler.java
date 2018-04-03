package com.mantledillusion.vaadin.cotton.model;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.model.ValidationError.ValidityLevel;

/**
 * Framework internal type <b>(DO NOT USE!)</b> for types that can validate a
 * model and show the found errors on the ui components bound to the respecting
 * {@link ModelProperty}s.
 *
 * @param <ModelType>
 *            The root type of the data model the {@link ModelValidationHandler}
 *            is able to validate.
 */
abstract class ModelValidationHandler<ModelType> extends ModelPersistingHandler<ModelType> {

	ModelValidationHandler() {
	}

	// ######################################################################################################################################
	// ############################################################ VALIDATION ##############################################################
	// ######################################################################################################################################

	/**
	 * Validates the model against the given context.
	 * <P>
	 * The errors found by the {@link PropertyValidator}s in the given
	 * {@link ValidationContext} will be displayed on the ui components bound to the
	 * properties that were invalid.
	 * <P>
	 * Throws a {@link WebException} when this does not contain a model at the
	 * moment.
	 * 
	 * @param context
	 *            The context to validate against; <b>not</b> allowed to be null.
	 * @return The determined level of validity; never null
	 */
	public final synchronized ValidityLevel validate(ValidationContext<ModelType> context) {
		if (context == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot validate against a null validation context.");
		} else if (!hasModel()) {
			throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR, "Cannot validate against a null model.");
		}

		ValidationErrorRegistry<ModelType> errorRegistry = new ValidationErrorRegistry<>();
		gatherPreevalutationErrors(errorRegistry);
		ValidityLevel validity = errorRegistry.getValidity();
		if (validity.isValid()) {
			context.validate(this, errorRegistry);
			applyErrors(errorRegistry);
			return errorRegistry.getValidity();
		} else {
			return validity;
		}
	}

	abstract void gatherPreevalutationErrors(ValidationErrorRegistry<ModelType> errorRegistry);

	abstract void applyErrors(ValidationErrorRegistry<ModelType> errorRegistry);

	/**
	 * Clears all current errors from this and the ui components they are displayed
	 * on.
	 */
	public final synchronized void clearValidation() {
		clearErrors();
	}

	abstract void clearErrors();
}
