package com.mantledillusion.vaadin.cotton.model;

import java.lang.reflect.Method;

import com.mantledillusion.data.epiphy.interfaces.ListedProperty;
import com.mantledillusion.data.epiphy.interfaces.ReadableProperty;
import com.mantledillusion.data.epiphy.interfaces.WriteableProperty;

/**
 * Interface for types that can hold a model and extract/change parts of it
 * using {@link ReadableProperty}s.
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

	/**
	 * Determines whether the given property exists in the model; or to put it
	 * differently, whether the parent properties of the property are all non-null.
	 * <p>
	 * The {@link Method} checks on the property parent's values, not on the
	 * properties' own value. If all parents are non-null but the property itself is
	 * null, the {@link Method} will still return true.
	 * <p>
	 * The result indicates whether it is safe to execute writing operations on the
	 * property.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the returned result.
	 * <P>
	 * For determination of existence, this handler's own index context is used.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to check.
	 * @param property
	 *            The property to check for existence; <b>not</b> allowed to be
	 *            null.
	 * @return True if all of the given properties' parents are non-null, false
	 *         otherwise
	 */
	<PropertyType> boolean exists(ReadableProperty<ModelType, PropertyType> property);

	/**
	 * Determines whether the given property exists in the model; or to put it
	 * differently, whether the parent properties of the property are all non-null.
	 * <p>
	 * The {@link Method} checks on the property parent's values, not on the
	 * properties' own value. If all parents are non-null but the property itself is
	 * null, the {@link Method} will still return true.
	 * <p>
	 * The result indicates whether it is safe to execute writing operations on the
	 * property.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the returned result.
	 * <P>
	 * For determination of existence, the given index context is used as an
	 * extension to the handler's own index context.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to check.
	 * @param property
	 *            The property to check for existence; <b>not</b> allowed to be
	 *            null.
	 * @param context
	 *            The context which is used for determining the correct property;
	 *            might be null.
	 * @return True if all of the given properties' parents are non-null, false
	 *         otherwise
	 */
	<PropertyType> boolean exists(ReadableProperty<ModelType, PropertyType> property, IndexContext context);

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
	 * @param <PropertyType>
	 *            The type of the property to get.
	 * @param property
	 *            The property to fetch model data for; <b>not</b> allowed to be
	 *            null.
	 * @return The target data in the model the given property points to; might be
	 *         null if the property is null
	 */
	<PropertyType> PropertyType getProperty(ReadableProperty<ModelType, PropertyType> property);

	/**
	 * Fetches the value from inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the returned result.
	 * <P>
	 * For determining the correct property, the given index context is used as an
	 * extension to the handler's own index context.
	 * 
	 * @param <PropertyType>
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
	<PropertyType> PropertyType getProperty(ReadableProperty<ModelType, PropertyType> property, IndexContext context);

	/**
	 * Sets the value inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the execution's result.
	 * <P>
	 * For determining the correct property, this handler's own index context is
	 * used.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to set.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @param value
	 *            The value to inject into the model.
	 */
	<PropertyType> void setProperty(WriteableProperty<ModelType, PropertyType> property, PropertyType value);

	/**
	 * Sets the value inside the model data the given property points to.
	 * <P>
	 * Note that if the path from the property model's root to the given property is
	 * indexed, the used index context has an impact on the execution's result.
	 * <P>
	 * For determining the correct property, the given index context is used as an
	 * extension to the handler's own index context.
	 * 
	 * @param <PropertyType>
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
	<PropertyType> void setProperty(WriteableProperty<ModelType, PropertyType> property, PropertyType value,
			IndexContext context);

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
	 * @param <PropertyType>
	 *            The type of the property to add.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @param value
	 *            The value to insert into the list; might be null.
	 */
	<PropertyType> void addProperty(ListedProperty<ModelType, PropertyType> property, PropertyType value);

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
	 * @param <PropertyType>
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
	<PropertyType> void addProperty(ListedProperty<ModelType, PropertyType> property, PropertyType value,
			IndexContext context);

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
	 * @param <PropertyType>
	 *            The type of the property to remove.
	 * @param property
	 *            The property to set inside the model; <b>not</b> allowed to be
	 *            null.
	 * @return The item that has been removed from the list; might be null if the
	 *         property is null
	 */
	<PropertyType> PropertyType removeProperty(ListedProperty<ModelType, PropertyType> property);

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
	 * @param <PropertyType>
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
	<PropertyType> PropertyType removeProperty(ListedProperty<ModelType, PropertyType> property, IndexContext context);
}
