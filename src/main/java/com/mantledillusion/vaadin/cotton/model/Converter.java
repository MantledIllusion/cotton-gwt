package com.mantledillusion.vaadin.cotton.model;

import java.util.Arrays;
import java.util.Collection;

import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.data.ValueContext;
import com.vaadin.server.SerializableFunction;

/**
 * Interface for {@link Converter}s that convert between the type of a property
 * and the field it is bound to.
 * <p>
 * By default, the build in {@link InputValidator} will never intervene the
 * conversion, override {@link #validateInput(Object)} if it has to.
 *
 * @param <FieldValueType>
 *            The value type of the field.
 * @param <PropertyType>
 *            The type of the property.
 */
public interface Converter<FieldValueType, PropertyType> extends InputValidator<FieldValueType> {

	/**
	 * Converts the property value to the field's value type.
	 * 
	 * @param value
	 *            The value to convert; might be null if the property is null
	 * @return The converted value; might be null
	 */
	FieldValueType toField(PropertyType value);

	/**
	 * Convertes the field value to the properties' value type.
	 * 
	 * @param value
	 *            The value to convert; might be null if the field's empty
	 *            representation is null.
	 * @return The converted value; might be null
	 */
	PropertyType toProperty(FieldValueType value);

	default Collection<ValidationError> validateInput(FieldValueType value) {
		return null;
	}

	/**
	 * Wraps an existing {@link com.vaadin.data.Converter} so it can be re-used as a
	 * {@link Converter}.
	 * <p>
	 * The resulting converter will wrap the given converter's error messages as a
	 * negative {@link #validateInput(Object)} result.
	 * 
	 * @param <FieldValueType>
	 *            The value type of the field.
	 * @param <PropertyType>
	 *            The type of the property.
	 * @param vaadinConverter
	 *            The converter to wrap; might <b>not</b> be null.
	 * @return A {@link Converter}, wrapping the given Vaadin one
	 */
	public static <FieldValueType, PropertyType> Converter<FieldValueType, PropertyType> wrap(
			com.vaadin.data.Converter<FieldValueType, PropertyType> vaadinConverter) {
		if (vaadinConverter == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Cannot wrap a null converter.");
		}
		return new Converter<FieldValueType, PropertyType>() {

			private final ValueContext context = new ValueContext();
			private final SerializableFunction<String, RuntimeException> errorFunction = message -> new RuntimeException(
					message);

			@Override
			public FieldValueType toField(PropertyType value) {
				return vaadinConverter.convertToPresentation(value, this.context);
			}

			@Override
			public PropertyType toProperty(FieldValueType value) {
				return vaadinConverter.convertToModel(value, this.context).getOrThrow(this.errorFunction);
			}

			@Override
			public Collection<ValidationError> validateInput(FieldValueType value) {
				String[] errorMsg = new String[1];
				vaadinConverter.convertToModel(value, this.context).ifError(errorMessage -> errorMsg[0] = errorMessage);
				return errorMsg[0] == null ? null : Arrays.asList(ValidationError.of(WebEnv.localize(errorMsg[0])));
			}
		};
	}
}