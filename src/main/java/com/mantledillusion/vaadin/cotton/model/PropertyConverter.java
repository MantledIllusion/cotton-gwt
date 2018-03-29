package com.mantledillusion.vaadin.cotton.model;

import com.vaadin.data.Converter;
import com.vaadin.data.ValueContext;
import com.vaadin.server.SerializableFunction;

public interface PropertyConverter<FieldType, PropertyTargetType> {

	FieldType toField(PropertyTargetType value);

	PropertyTargetType toProperty(FieldType value);

	static <FieldType, PropertyTargetType> PropertyConverter<FieldType, PropertyTargetType> wrap(Converter<FieldType, PropertyTargetType> vaadinConverter) {
		return new PropertyConverter<FieldType, PropertyTargetType>() {
			
			private final ValueContext context = new ValueContext();
			private final SerializableFunction<String, RuntimeException> errorFunction = message -> new RuntimeException(message);

			@Override
			public FieldType toField(PropertyTargetType value) {
				return vaadinConverter.convertToPresentation(value, this.context);
			}

			@Override
			public PropertyTargetType toProperty(FieldType value) {
				return vaadinConverter.convertToModel(value, this.context).getOrThrow(this.errorFunction);
			}
		};
	}
}