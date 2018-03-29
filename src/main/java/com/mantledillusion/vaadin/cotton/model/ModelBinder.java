package com.mantledillusion.vaadin.cotton.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.data.epiphy.ModelPropertyList;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory.OptionPattern;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.data.HasValue;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.shared.Registration;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.Label;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

/**
 * Framework internal type <b>(DO NOT USE!)</b> for types that can bind ui
 * components to {@link ModelProperty}s.
 * 
 * @param <ModelType>
 *            The root type of the data model the {@link ModelBinder} is able to
 *            bind ui components to.
 */
abstract class ModelBinder<ModelType> extends ModelProxy<ModelType> {

	private interface HasValueProvider<FieldType extends Component & HasValue<FieldValueType>, FieldValueType> {

		FieldType build(@SuppressWarnings("unchecked") OptionPattern<? super FieldType>... patterns);
	}

	private interface Getter<PropertyTargetType> {

		PropertyTargetType get();
	}

	private interface Setter<PropertyTargetType> {

		void set(PropertyTargetType value);
	}

	private static final class HasValueDelegate<PropertyTargetType> {

		private final HasValue<?> field;
		private final Getter<PropertyTargetType> getter;
		private final Setter<PropertyTargetType> setter;
		private final Getter<PropertyTargetType> emptyRepresentationGetter;
		private final Setter<ErrorMessage> errorSetter;

		private HasValueDelegate(HasValue<?> field, Getter<PropertyTargetType> getter,
				Setter<PropertyTargetType> setter, Getter<PropertyTargetType> emptyRepresentationProvider) {
			this.field = field;
			this.getter = getter;
			this.setter = setter;
			this.emptyRepresentationGetter = emptyRepresentationProvider;
			if (this.field instanceof AbstractComponent) {
				this.errorSetter = errorMessage -> ((AbstractComponent) this.field).setComponentError(errorMessage);
			} else {
				this.errorSetter = errorMessage -> {};
			}
		}

		PropertyTargetType getValue() {
			return this.getter.get();
		}

		void setValue(PropertyTargetType value) {
			if (value == null) {
				this.setter.set(this.emptyRepresentationGetter.get());
			} else {
				this.setter.set(value);
			}
		}

		void setReadOnly(boolean readOnly) {
			this.field.setReadOnly(readOnly);
		}
		
		void setError(ErrorMessage errorMessage) {
			this.errorSetter.set(errorMessage);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		Registration register(ValueChangeListener listener) {
			return this.field.addValueChangeListener(listener);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	final class PropertyBinding<PropertyTargetType> implements ValueChangeListener {

		private static final long serialVersionUID = 1L;

		private final ModelProperty<ModelType, PropertyTargetType> property;
		private final HasValueDelegate<PropertyTargetType> hasValue;
		private final Registration registration;

		private PropertyBinding(ModelProperty<ModelType, PropertyTargetType> property,
				HasValueDelegate<PropertyTargetType> hasValue) {
			this.property = property;
			this.hasValue = hasValue;
			this.registration = this.hasValue.register(this);
		}

		private boolean updating = false;

		@Override
		public void valueChange(ValueChangeEvent event) {
			if (!this.updating && ModelBinder.this.exists(this.property)) {
				ModelBinder.this.setProperty(this.property, this.hasValue.getValue());
			}
		}

		synchronized void update() {
			this.updating = true;
			this.hasValue.setReadOnly(!ModelBinder.this.exists(this.property));
			this.hasValue.setValue(ModelBinder.this.getProperty(this.property));
			this.updating = false;
		}
		
		void setError(Map<ModelProperty<?, ?>, Set<String>> errorMessages) {
			if (errorMessages.containsKey(property)) {
				this.hasValue.setError(new UserError(StringUtils.join(errorMessages.get(property), ',')));
			} else {
				clearError();
			}
		}
		
		void clearError() {
			this.hasValue.setError(null);
		}
		
		void destroy() {
			this.registration.remove();
		}
	}

	<PropertyTargetType> PropertyBinding<PropertyTargetType> of(ModelProperty<ModelType, PropertyTargetType> property,
			HasValue<PropertyTargetType> field) {
		return new PropertyBinding<>(property, new HasValueDelegate<>(field, field::getValue, field::setValue, field::getEmptyValue));
	}

	<PropertyTargetType, FieldValueType> PropertyBinding<PropertyTargetType> of(
			ModelProperty<ModelType, PropertyTargetType> property, HasValue<FieldValueType> field,
			PropertyConverter<FieldValueType, PropertyTargetType> converter) {
		return new PropertyBinding<>(property, new HasValueDelegate<>(field,
				() -> converter.toProperty(field.getValue()), 
				(value) -> field.setValue(converter.toField(value)),
				() -> converter.toProperty(field.getEmptyValue())));
	}

	ModelBinder() {
	}

	private <FieldType extends Component & HasValue<FieldValueType>, FieldValueType> FieldType buildAndBind(
			HasValueProvider<FieldType, FieldValueType> provider, ModelProperty<ModelType, FieldValueType> property,
			@SuppressWarnings("unchecked") OptionPattern<? super FieldType>... patterns) {
		if (property == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot bind a component for a null property.");
		}
		FieldType comp = provider.build(patterns);
		ModelBinder.this.bindToProperty(comp, property);
		return comp;
	}

	/**
	 * Binds the given field to the given property.
	 * 
	 * @param <FieldType>
	 *            The type of {@link HasValue} that is being bound.
	 * @param <PropertyType>
	 *            The type of data the property refers to.
	 * @param field
	 *            The {@link HasValue} to bind; might <b>not</b> be null.
	 * @param property
	 *            The property to bind to; might <b>not</b> be null.
	 * @return The given field for in-line use
	 */
	public abstract <FieldType extends HasValue<PropertyType>, PropertyType> FieldType bindToProperty(FieldType field,
			ModelProperty<ModelType, PropertyType> property);

	private <FieldType extends Component & HasValue<FieldValueType>, FieldValueType, PropertyType> FieldType buildAndBind(
			HasValueProvider<FieldType, FieldValueType> provider, ModelProperty<ModelType, PropertyType> property,
			PropertyConverter<FieldValueType, PropertyType> converter,
			@SuppressWarnings("unchecked") OptionPattern<? super FieldType>... patterns) {
		if (property == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot bind a component for a null property.");
		} else if (converter == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot bind a component to a differently typed property using a null converter.");
		}
		FieldType comp = provider.build(patterns);
		ModelBinder.this.bindToProperty(comp, property, converter);
		return comp;
	}

	/**
	 * Binds the given field to the given property.
	 * 
	 * @param <FieldType>
	 *            The type of {@link HasValue} that is being bound.
	 * @param <FieldValueType>
	 *            The type of value the field accepts
	 * @param <PropertyValueType>
	 *            The type of data the property refers to.
	 * @param field
	 *            The {@link HasValue} to bind; might <b>not</b> be null.
	 * @param property
	 *            The property to bind to; might <b>not</b> be null.
	 * @param converter
	 *            The converter needed for map from the field's value type to the
	 *            properties' value type and vice versa; might <b>not</b> be null.
	 * @return The given field for in-line use
	 */
	public abstract <FieldType extends HasValue<FieldValueType>, FieldValueType, PropertyValueType> FieldType bindToProperty(
			FieldType field, ModelProperty<ModelType, PropertyValueType> property,
			PropertyConverter<FieldValueType, PropertyValueType> converter);

	// ##############################################################################################################
	// ################################################## LABEL #####################################################
	// ##############################################################################################################

	/**
	 * Directly builds a single {@link Label} and binds it.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link Label} to;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link Label} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <PropertyType> Label bindLabelForProperty(ModelProperty<ModelType, PropertyType> property,
			OptionPattern<? super Label>... patterns) {
		return bindLabelForProperty(property, StringRenderer.defaultRenderer(), patterns);
	}

	/**
	 * Directly builds a single {@link Label} and binds it.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link Label} to;
	 *            <b>not</b> allowed to be null.
	 * @param renderer
	 *            The renderer to convert the property type to Strings used by the
	 *            {@link Label}; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link Label} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <PropertyType> Label bindLabelForProperty(ModelProperty<ModelType, PropertyType> property,
			StringRenderer<PropertyType> renderer, OptionPattern<? super Label>... patterns) {
		if (renderer == null) {
			throw new IllegalArgumentException("Cannot apply a null renderer.");
		}
		HasValueProvider<BindableLabel, String> provider = (p) -> ComponentFactory.apply(new BindableLabel(), p);
		PropertyConverter<String, PropertyType> converter = new PropertyConverter<String, PropertyType>() {

			@Override
			public PropertyType toProperty(String value) {
				throw new UnsupportedOperationException("Cannot convert a label's text to model data.");
			}

			@Override
			public String toField(PropertyType value) {
				return renderer.render(value);
			}
		};
		return buildAndBind(provider, property, converter, patterns);
	}

	// ##############################################################################################################
	// ################################################ TEXTFIELD ###################################################
	// ##############################################################################################################

	/**
	 * Directly builds a single {@link TextField} and binds it.
	 * 
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link TextField} to;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link TextField} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final TextField bindTextFieldForProperty(ModelProperty<ModelType, String> property,
			OptionPattern<? super TextField>... patterns) {
		return buildAndBind(ComponentFactory::buildTextField, property, patterns);
	}

	/**
	 * Directly builds a single {@link TextField} and binds it.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link TextField} to;
	 *            <b>not</b> allowed to be null.
	 * @param converter
	 *            The converter to convert the property type to the type used by the
	 *            {@link TextField}; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link TextField} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <PropertyType> TextField bindTextFieldForProperty(ModelProperty<ModelType, PropertyType> property,
			PropertyConverter<String, PropertyType> converter, OptionPattern<? super TextField>... patterns) {
		return buildAndBind(ComponentFactory::buildTextField, property, converter, patterns);
	}

	// ##############################################################################################################
	// ################################################# TEXTAREA ###################################################
	// ##############################################################################################################

	/**
	 * Directly builds a single {@link TextArea} and binds it.
	 * 
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link TextArea} to;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link TextArea} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final TextArea bindTextAreaForProperty(ModelProperty<ModelType, String> property,
			OptionPattern<? super TextArea>... patterns) {
		return buildAndBind(ComponentFactory::buildTextArea, property, patterns);
	}

	/**
	 * Directly builds a single {@link TextArea} and binds it.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link TextArea} to;
	 *            <b>not</b> allowed to be null.
	 * @param converter
	 *            The converter to convert the property type to the type used by the
	 *            {@link TextArea}; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link TextArea} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <PropertyType> TextArea bindTextAreaForProperty(ModelProperty<ModelType, PropertyType> property,
			PropertyConverter<String, PropertyType> converter, OptionPattern<? super TextArea>... patterns) {
		return buildAndBind(ComponentFactory::buildTextArea, property, converter, patterns);
	}

	// ##############################################################################################################
	// ################################################ DATEFIELD ###################################################
	// ##############################################################################################################

	/**
	 * Directly builds a single {@link DateField} and binds it.
	 * 
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link DateField} to;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link DateField} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final DateField bindDateFieldForProperty(ModelProperty<ModelType, LocalDate> property,
			OptionPattern<? super DateField>... patterns) {
		return buildAndBind(ComponentFactory::buildDateField, property, patterns);
	}

	/**
	 * Directly builds a single {@link DateField} and binds it.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link DateField} to;
	 *            <b>not</b> allowed to be null.
	 * @param converter
	 *            The converter to convert the property type to the type used by the
	 *            {@link DateField}; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link DateField} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <PropertyType> DateField bindDateFieldForProperty(ModelProperty<ModelType, PropertyType> property,
			PropertyConverter<LocalDate, PropertyType> converter, OptionPattern<? super DateField>... patterns) {
		return buildAndBind(ComponentFactory::buildDateField, property, converter, patterns);
	}

	/**
	 * Directly builds a single {@link DateTimeField} and binds it.
	 * 
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link DateTimeField}
	 *            to; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link DateTimeField} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final DateTimeField bindDateTimeFieldForProperty(ModelProperty<ModelType, LocalDateTime> property,
			OptionPattern<? super DateTimeField>... patterns) {
		return buildAndBind(ComponentFactory::buildDateTimeField, property, patterns);
	}

	/**
	 * Directly builds a single {@link DateTimeField} and binds it.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link DateTimeField}
	 *            to; <b>not</b> allowed to be null.
	 * @param converter
	 *            The converter to convert the property type to the type used by the
	 *            {@link DateTimeField}; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link DateTimeField} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <PropertyType> DateTimeField bindDateTimeFieldForProperty(
			ModelProperty<ModelType, PropertyType> property, PropertyConverter<LocalDateTime, PropertyType> converter,
			OptionPattern<? super DateTimeField>... patterns) {
		return buildAndBind(ComponentFactory::buildDateTimeField, property, converter, patterns);
	}

	// ##############################################################################################################
	// ################################################# CHECKBOX ###################################################
	// ##############################################################################################################

	/**
	 * Directly builds a single {@link CheckBox} and binds it.
	 * 
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link CheckBox} to;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link CheckBox} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final CheckBox bindCheckBoxForProperty(ModelProperty<ModelType, Boolean> property,
			OptionPattern<? super CheckBox>... patterns) {
		return buildAndBind(ComponentFactory::buildCheckBox, property, patterns);
	}

	/**
	 * Directly builds a single {@link CheckBox} and binds it.
	 * 
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link CheckBox} to;
	 *            <b>not</b> allowed to be null.
	 * @param converter
	 *            The converter to convert the property type to the type used by the
	 *            {@link CheckBox}; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link CheckBox} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <PropertyType> CheckBox bindCheckBoxForProperty(ModelProperty<ModelType, PropertyType> property,
			PropertyConverter<Boolean, PropertyType> converter, OptionPattern<? super CheckBox>... patterns) {
		return buildAndBind(ComponentFactory::buildCheckBox, property, converter, patterns);
	}

	// ##############################################################################################################
	// ############################################ RADIOBUTTON GROUP ###############################################
	// ##############################################################################################################

	/**
	 * Directly builds a single {@link RadioButtonGroup} and binds it.
	 * 
	 * @param <T>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link RadioButtonGroup}
	 *            to; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link RadioButtonGroup} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <T> RadioButtonGroup<T> bindRadioButtonGroupForProperty(ModelProperty<ModelType, T> property,
			OptionPattern<? super RadioButtonGroup<?>>... patterns) {
		HasValueProvider<RadioButtonGroup<T>, T> provider = ComponentFactory::buildRadioButtonGroup;
		return buildAndBind(provider, property, patterns);
	}

	/**
	 * Directly builds a single {@link RadioButtonGroup} and binds it.
	 * 
	 * @param <T>
	 *            The type to convert the property type to.
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link RadioButtonGroup}
	 *            to; <b>not</b> allowed to be null.
	 * @param converter
	 *            The converter to convert the property type to the type used by the
	 *            {@link RadioButtonGroup}; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link RadioButtonGroup} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <T, PropertyType> RadioButtonGroup<T> bindRadioButtonGroupForProperty(
			ModelProperty<ModelType, PropertyType> property, PropertyConverter<T, PropertyType> converter,
			OptionPattern<? super RadioButtonGroup<?>>... patterns) {
		HasValueProvider<RadioButtonGroup<T>, T> provider = ComponentFactory::buildRadioButtonGroup;
		return buildAndBind(provider, property, converter, patterns);
	}

	// ##############################################################################################################
	// ################################################# COMBOBOX ###################################################
	// ##############################################################################################################

	/**
	 * Directly builds a single {@link ComboBox} and binds it.
	 * 
	 * @param <T>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link ComboBox} to;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link ComboBox} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <T> ComboBox<T> bindComboBoxForProperty(ModelProperty<ModelType, T> property,
			OptionPattern<? super ComboBox<?>>... patterns) {
		HasValueProvider<ComboBox<T>, T> provider = ComponentFactory::buildComboBox;
		return buildAndBind(provider, property, patterns);
	}

	/**
	 * Directly builds a single {@link ComboBox} and binds it.
	 * 
	 * @param <T>
	 *            The type to convert the property type to.
	 * @param <PropertyType>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link ComboBox} to;
	 *            <b>not</b> allowed to be null.
	 * @param converter
	 *            The converter to convert the property type to the type used by the
	 *            {@link ComboBox}; <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link ComboBox} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <T, PropertyType> ComboBox<T> bindComboBoxForProperty(ModelProperty<ModelType, PropertyType> property,
			PropertyConverter<T, PropertyType> converter, OptionPattern<? super ComboBox<?>>... patterns) {
		HasValueProvider<ComboBox<T>, T> provider = ComponentFactory::buildComboBox;
		return buildAndBind(provider, property, converter, patterns);
	}

	// ##############################################################################################################
	// ################################################### TABLE ####################################################
	// ##############################################################################################################

	/**
	 * Directly builds a single {@link BindableGrid} and binds it.
	 * 
	 * @param <T>
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link BindableGrid} to;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link BindableGrid} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <T> BindableGrid<T, ModelType> bindGridForProperty(ModelPropertyList<ModelType, T> property,
			OptionPattern<? super BindableGrid<?, ?>>... patterns) {
		BindableGrid<T, ModelType> table = new BindableGrid<T, ModelType>(this, property);
		bindToProperty(table.getBindable(), property);
		return ComponentFactory.apply(table, patterns);
	}
}