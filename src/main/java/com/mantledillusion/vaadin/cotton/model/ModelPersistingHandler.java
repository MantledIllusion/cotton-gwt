package com.mantledillusion.vaadin.cotton.model;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.vaadin.cotton.exception.WebException;

/**
 * Framework internal type <b>(DO NOT USE!)</b> for types that can persist a
 * model or just some parts of it using a change log containing changed
 * {@link ModelProperty}s.
 *
 * @param <ModelType>
 *            The root type of the data model the {@link ModelValidationHandler}
 *            is able to persist.
 */
abstract class ModelPersistingHandler<ModelType> implements ModelHandler<ModelType> {

	ModelPersistingHandler() {
	}

	// ######################################################################################################################################
	// ########################################################### MODEL CONTROL ############################################################
	// ######################################################################################################################################

	/**
	 * Returns whether the model instance currently contained by this handler has
	 * changed since the model was added to it or the property change log was
	 * reseted the last time.
	 * 
	 * @return True if there are pending changes to the model, false otherwise or if
	 *         there is no current model
	 */
	public abstract boolean isModelChanged();

	/**
	 * Returns whether the given specific property of the model instance currently
	 * contained by this handler or (any sub-property) has changed since the model
	 * was added to it or the property change log was reseted the last time.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the returned result. If
	 * there are multiple indexes needed for the given property but only one is
	 * contained in the used context, a logged change on any index of that missing
	 * context index counts as changed.
	 * <P>
	 * For determination on changes, this handler's own index context is used.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to check for changes.
	 * @param property
	 *            The property to check for changes for; <b>not</b> allowed to be
	 *            null.
	 * @return True if there are pending changes to the given property, false
	 *         otherwise or if there is no current model
	 */
	public abstract <PropertyType> boolean isPropertyChanged(
			ModelProperty<ModelType, PropertyType> property);

	/**
	 * Returns whether the given specific property of the model instance currently
	 * contained by this handler or (any sub-property) has changed since the model
	 * was added to it or the property change log was reseted the last time.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the returned result. If
	 * there are multiple indexes needed for the given property but only one is
	 * contained in the used context, a logged change on any index of that missing
	 * context index counts as changed.
	 * <P>
	 * For determination on changes, the given index context is used.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to check for changes.
	 * @param property
	 *            The property to check for changes for; <b>not</b> allowed to be
	 *            null.
	 * @param context
	 *            The context which is used for determining the property to check;
	 *            might be null.
	 * @return True if there are pending changes to the given property, false
	 *         otherwise or if there is no current model
	 */
	public abstract <PropertyType> boolean isPropertyChanged(
			ModelProperty<ModelType, PropertyType> property, IndexContext context);

	// ######################################################################################################################################
	// ############################################################ PERSISTING ##############################################################
	// ######################################################################################################################################

	/**
	 * Persists all changes of the model that are pending and updates the current
	 * model with the {@link ModelPersistor}s' results.
	 * <P>
	 * Throws a {@link WebException} when there is no model at the moment or there
	 * are changes for one or more properties where there is no
	 * {@link ModelPersistor} registered.
	 * <P>
	 * Note that the index context used has an impact on the execution for every of
	 * the models properties whose paths from the property model's root are indexed.
	 * <P>
	 * For determining the properties to persist, this {@link ModelContainer}'s own
	 * index context is used.
	 * <P>
	 * For the whole set of changed instances to persist, the registered
	 * {@link ModelPersistor}s are used to determine the most effective way to
	 * persist all of them without having to persist data that is unchanged. For
	 * example, if there are two {@link ModelPersistor}s for the root model and one
	 * sub property, and that sub property is the only thing changed, only that sub
	 * properties' {@link ModelPersistor} will be used.
	 * <P>
	 * Since the pending changes combined with the {@link ModelPersistor}s
	 * registered determine what parts of the model will be persisted, it is not
	 * possible to know beforehand if the whole model or just parts are going to be
	 * persisted, so the whole persisted model is returned.
	 * 
	 * @return The persisted and updated model.
	 */
	public abstract ModelType persist();

	/**
	 * Persist all changes of the model that are pending and updates the current
	 * model with the {@link ModelPersistor}s' results.
	 * <P>
	 * Throws a {@link WebException} when there is no model at the moment or there
	 * are changes for one or more properties where there is no
	 * {@link ModelPersistor} registered.
	 * <P>
	 * Note that the index context used has an impact on the execution for every of
	 * the models properties whose paths from the property model's root are indexed.
	 * <P>
	 * For determining the properties to persist, the given index context is used.
	 * <P>
	 * For the whole set of changed instances to persist, the registered
	 * {@link ModelPersistor}s are used to determine the most effective way to
	 * persist all of them without having to persist data that is unchanged. For
	 * example, if there are two {@link ModelPersistor}s for the root model and one
	 * sub property, and that sub property is the only thing changed, only that sub
	 * properties' {@link ModelPersistor} will be used.
	 * <P>
	 * Since the pending changes combined with the {@link ModelPersistor}s
	 * registered determine what parts of the model will be persisted, it is not
	 * possible to know beforehand if the whole model or just parts are going to be
	 * persisted, so the whole persisted model is returned.
	 * 
	 * @param context
	 *            The context which is used for determining the properties to
	 *            persist property; might be null.
	 * @return The persisted and updated model.
	 */
	public abstract ModelType persist(IndexContext context);

}
