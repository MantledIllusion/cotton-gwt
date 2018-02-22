package com.mantledillusion.vaadin.cotton.model;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.data.epiphy.ModelPropertyList;

/**
 * Interface for types that can hold a model and extract/change parts of it
 * using {@link ModelProperty}s.
 * <P>
 * The default implementations to use are:<BR>
 * - {@link ModelContainer}: To hold a model and allow access to it<BR>
 * - {@link ModelAccessor}: For indexed proxying to underneath a
 * {@link ModelContainer}
 *
 * @param <ModelType>
 *            The root type of the data model the {@link ModelHandler} is able
 *            to hold.
 */
public interface ModelHandler<ModelType> {

	// ######################################################################################################################################
	// ########################################################### MODEL CONTROL ############################################################
	// ######################################################################################################################################

	/**
	 * Returns whether this currently contains a model instance.
	 * 
	 * @return True if this currently contains a model instance, false otherwise
	 */
	boolean hasModel();

	/**
	 * Returns the model instance currently contained.
	 * 
	 * @return The current model; might be null if there is no model
	 */
	ModelType getModel();

	// ######################################################################################################################################
	// ###################################################### PROPERTIED MODEL ACCESS #######################################################
	// ######################################################################################################################################

	/**
	 * Fetches the value from inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the returned result.
	 * <P>
	 * For determining the correct property, this handler's own index context is
	 * used.
	 * 
	 * @param <TargetPropertyType>
	 *            The type of the property to get.
	 * @param property
	 *            The property to fetch model data for; <b>not</b> allowed to be
	 *            null.
	 * @return The target data in the model the given property points to; might be
	 *         null if the property is null
	 */
	<TargetPropertyType> TargetPropertyType getProperty(ModelProperty<ModelType, TargetPropertyType> property);

	/**
	 * Fetches the value from inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the returned result.
	 * <P>
	 * For determining the correct property, the given index context is used as an
	 * extension to the handler's own index context.
	 * 
	 * @param <TargetPropertyType>
	 *            The type of the property to get.
	 * @param property
	 *            The property to fetch model data for; <b>not</b> allowed to be
	 *            null.
	 * @param context
	 *            The context which is used for determining the correct property;
	 *            might be null.
	 * @return The target data in the model the given property points to; might be
	 *         null if the property is null
	 */
	<TargetPropertyType> TargetPropertyType getProperty(ModelProperty<ModelType, TargetPropertyType> property,
			IndexContext context);

	/**
	 * Sets the value inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the execution's result.
	 * <P>
	 * For determining the correct property, this handler's own index context is
	 * used.
	 * 
	 * @param <TargetPropertyType>
	 *            The type of the property to set.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @param value
	 *            The value to inject into the model.
	 */
	<TargetPropertyType> void setProperty(ModelProperty<ModelType, TargetPropertyType> property,
			TargetPropertyType value);

	/**
	 * Sets the value inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the execution's result.
	 * <P>
	 * For determining the correct property, the given index context is used as an
	 * extension to the handler's own index context.
	 * 
	 * @param <TargetPropertyType>
	 *            The type of the property to set.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @param value
	 *            The value to inject into the model; might be null.
	 * @param context
	 *            The context which is used for determining the correct property;
	 *            might be null.
	 */
	<TargetPropertyType> void setProperty(ModelProperty<ModelType, TargetPropertyType> property,
			TargetPropertyType value, IndexContext context);

	/**
	 * Adds the value to a list inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the execution's result. The
	 * index of the given property also determines what will be the index of the
	 * given value in the list after adding.
	 * <P>
	 * For determining the correct property, this handler's own index context is
	 * used.
	 * 
	 * @param <TargetPropertyType>
	 *            The type of the property to add.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @param value
	 *            The value to insert into the list; might be null.
	 */
	<TargetPropertyType> void addProperty(ModelPropertyList<ModelType, TargetPropertyType> property,
			TargetPropertyType value);

	/**
	 * Adds an item to a list inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the execution's result. The
	 * index of the given property also determines what will be the index of the
	 * given value in the list after adding.
	 * <P>
	 * For determining the correct property, the given index context is used as an
	 * extension to the handler's own index context.
	 * 
	 * @param <TargetPropertyType>
	 *            The type of the property to add.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @param value
	 *            The value to insert into the list; might be null.
	 * @param context
	 *            The context which is used for determining the correct property;
	 *            might be null.
	 */
	<TargetPropertyType> void addProperty(ModelPropertyList<ModelType, TargetPropertyType> property,
			TargetPropertyType value, IndexContext context);

	/**
	 * Removes an item from a list inside the model data the given property points
	 * to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the execution's result. The
	 * index of the given property also determines what the index of the item will
	 * be that is removed from the list.
	 * <P>
	 * For determining the correct property, this handler's own index context is
	 * used.
	 * 
	 * @param <TargetPropertyType>
	 *            The type of the property to remove.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @return The item that has been removed from the list; might be null if the
	 *         property is null
	 */
	<TargetPropertyType> TargetPropertyType removeProperty(ModelPropertyList<ModelType, TargetPropertyType> property);

	/**
	 * Removes an item from a list inside the model data the given property points
	 * to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the execution's result. The
	 * index of the given property also determines what the index of the item will
	 * be that is removed from the list.
	 * <P>
	 * For determining the correct property, the given index context is used as an
	 * extension to the handler's own index context.
	 * 
	 * @param <TargetPropertyType>
	 *            The type of the property to remove.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @param context
	 *            The context which is used for determining the correct property;
	 *            might be null.
	 * @return The item that has been removed from the list; might be null if the
	 *         property is null
	 */
	<TargetPropertyType> TargetPropertyType removeProperty(ModelPropertyList<ModelType, TargetPropertyType> property,
			IndexContext context);
}
