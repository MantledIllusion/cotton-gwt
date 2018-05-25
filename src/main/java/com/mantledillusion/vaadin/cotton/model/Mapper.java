package com.mantledillusion.vaadin.cotton.model;

/**
 * Interface for {@link Mapper}s that convert from the type of a property
 * and the field it is bound to.
 *
 * @param <FieldValueType>
 *            The value type of the field.
 * @param <PropertyType>
 *            The type of the property.
 */
@FunctionalInterface
public interface Mapper<FieldValueType, PropertyType> {

	/**
	 * Converts the property value to the field's value type.
	 * 
	 * @param value
	 *            The value to convert; might be null if the property is null
	 * @return The converted value; might be null
	 */
	FieldValueType toField(PropertyType value);
}