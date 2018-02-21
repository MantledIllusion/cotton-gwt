package com.mantledillusion.vaadin.cotton.model;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.vaadin.cotton.model.ValidationContext.ValidationErrorRegistry;

/**
 * A model {@link Validator}.
 *
 * @param <ModelType>
 *            The root type of the data model the {@link Validator} is able to
 *            validate.
 */
public interface Validator<ModelType> {

	/**
	 * Will be called when the containing {@link ValidationContext} is applied on a
	 * model.
	 * 
	 * @param handler
	 *            The {@link ModelHandler} validating the model,
	 *            can be used for {@link ModelProperty}ed access on the model.
	 * @param errorRegistry
	 *            The {@link ValidationErrorRegistry} to register found errors at.
	 */
	public void validate(ModelHandler<ModelType> handler,
			ValidationErrorRegistry<ModelType> errorRegistry);
}