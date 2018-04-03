package com.mantledillusion.vaadin.cotton.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.data.epiphy.ModelPropertyList;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory.OptionPattern;
import com.vaadin.data.HasValue;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.server.CompositeErrorMessage;
import com.vaadin.server.ErrorMessage;
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

	private interface Getter<PropertyType> {

		PropertyType get();
	}

	private interface Setter<PropertyType> {

		void set(PropertyType value);
	}

	private interface ErrorRetriever {

		Collection<ValidationError> validate();
	}

	private static final class HasValueDelegate<PropertyType> {

		private final HasValue<?> field;
		private final Getter<PropertyType> getter;
		private final Setter<PropertyType> setter;
		private final Getter<PropertyType> emptyRepresentationGetter;
		private final ErrorRetriever errorRetriever;
		private final Setter<ErrorMessage> errorSetter;

		private HasValueDelegate(HasValue<?> field, Getter<PropertyType> getter,
				Setter<PropertyType> setter, ErrorRetriever inputChecker,
				Getter<PropertyType> emptyRepresentationProvider) {
			this.field = field;
			this.getter = getter;
			this.setter = setter;
			this.errorRetriever = inputChecker;
			this.emptyRepresentationGetter = emptyRepresentationProvider;
			if (this.field instanceof AbstractComponent) {
				this.errorSetter = errorMessage -> ((AbstractComponent) this.field).setComponentError(errorMessage);
			} else {
				this.errorSetter = errorMessage -> {
				};
			}
		}

		PropertyType getValue() {
			return this.getter.get();
		}

		void setValue(PropertyType value) {
			if (value == null) {
				this.setter.set(this.emptyRepresentationGetter.get());
			} else {
				this.setter.set(value);
			}
		}

		void setReadOnly(boolean readOnly) {
			this.field.setReadOnly(readOnly);
		}

		Set<ValidationError> validate() {
			Collection<ValidationError> errors = this.errorRetriever.validate();
			if (errors != null) {
				Set<ValidationError> errorSet = new HashSet<>(errors);
				errorSet.remove(null);
				return Collections.unmodifiableSet(errorSet);
			} else {
				return Collections.emptySet();
			}
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
	final class PropertyBinding<PropertyType> implements ValueChangeListener {

		private static final long serialVersionUID = 1L;

		private final ModelProperty<ModelType, PropertyType> property;
		private final HasValueDelegate<PropertyType> hasValue;
		private final Registration registration;

		private PropertyBinding(ModelProperty<ModelType, PropertyType> property,
				HasValueDelegate<PropertyType> hasValue) {
			this.property = property;
			this.hasValue = hasValue;
			this.registration = this.hasValue.register(this);
		}

		private boolean updating = false;

		@Override
		public synchronized void valueChange(ValueChangeEvent event) {
			if (!this.updating && ModelBinder.this.exists(this.property)) {
				Set<ValidationError> errors = this.hasValue.validate();

				this.updating = true;
				switch (ValidationErrorRegistry.containsError(errors)) {
				case VALID:
					ModelBinder.this.setProperty(this.property, this.hasValue.getValue());
					this.hasValue.setError(null);
					break;
				case WARNING:
					ModelBinder.this.setProperty(this.property, this.hasValue.getValue());
					this.hasValue.setError(new CompositeErrorMessage(errors));
					break;
				case ERROR:
					ModelBinder.this.setProperty(this.property, null);
					this.hasValue.setError(new CompositeErrorMessage(errors));
					break;
				}
				
				this.updating = false;
			}
		}

		synchronized void update() {
			if (!this.updating) {
				this.updating = true;
				this.hasValue.setReadOnly(!ModelBinder.this.exists(this.property));
				this.hasValue.setValue(ModelBinder.this.getProperty(this.property));
				this.updating = false;
			}
		}

		Set<ValidationError> getError() {
			return this.hasValue.validate();
		}

		void setError(ValidationErrorRegistry<ModelType> errorRegistry) {
			if (errorRegistry.hasErrorsForProperty(this.property)) {
				this.hasValue.setError(new CompositeErrorMessage(errorRegistry.getErrorsOfProperty(this.property)));
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

	<PropertyType> PropertyBinding<PropertyType> bindingOf(
			ModelProperty<ModelType, PropertyType> property, HasValue<PropertyType> field) {
		return bindingOf(property, field, value -> null);
	}

	<PropertyType> PropertyBinding<PropertyType> bindingOf(
			ModelProperty<ModelType, PropertyType> property, HasValue<PropertyType> field,
			InputValidator<PropertyType> inputValidator) {
		return new PropertyBinding<>(property, new HasValueDelegate<>(field, field::getValue, field::setValue,
				() -> inputValidator.validateInput(field.getValue()), field::getEmptyValue));
	}

	<PropertyType, FieldValueType> PropertyBinding<PropertyType> bindingOf(
			ModelProperty<ModelType, PropertyType> property, HasValue<FieldValueType> field,
			Converter<FieldValueType, PropertyType> converter) {
		return new PropertyBinding<>(property, new HasValueDelegate<>(field,
				() -> converter.toProperty(field.getValue()), (value) -> field.setValue(converter.toField(value)),
				() -> converter.validateInput(field.getValue()), () -> converter.toProperty(field.getEmptyValue())));
	}

	ModelBinder() {
	}

	private <FieldType extends Component & HasValue<FieldValueType>, FieldValueType> FieldType buildAndBind(
			HasValueProvider<FieldType, FieldValueType> provider, ModelProperty<ModelType, FieldValueType> property,
			@SuppressWarnings("unchecked") OptionPattern<? super FieldType>... patterns) {
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

	private <FieldType extends Component & HasValue<FieldValueType>, FieldValueType> FieldType buildAndBind(
			HasValueProvider<FieldType, FieldValueType> provider, ModelProperty<ModelType, FieldValueType> property,
			InputValidator<FieldValueType> inputValidator,
			@SuppressWarnings("unchecked") OptionPattern<? super FieldType>... patterns) {
		FieldType comp = provider.build(patterns);
		ModelBinder.this.bindToProperty(comp, property, inputValidator);
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
	 * @param inputValidator
	 *            The validator used to validate the field's raw input; might
	 *            <b>not</b> be null.
	 * @return The given field for in-line use
	 */
	public abstract <FieldType extends HasValue<PropertyType>, PropertyType> FieldType bindToProperty(FieldType field,
			ModelProperty<ModelType, PropertyType> property, InputValidator<PropertyType> inputValidator);

	private <FieldType extends Component & HasValue<FieldValueType>, FieldValueType, PropertyType> FieldType buildAndBind(
			HasValueProvider<FieldType, FieldValueType> provider, ModelProperty<ModelType, PropertyType> property,
			Converter<FieldValueType, PropertyType> converter,
			@SuppressWarnings("unchecked") OptionPattern<? super FieldType>... patterns) {
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
			Converter<FieldValueType, PropertyValueType> converter);

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
		Converter<String, PropertyType> converter = new Converter<String, PropertyType>() {

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
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link TextField} to;
	 *            <b>not</b> allowed to be null.
	 * @param inputValidator
	 *            The validator to validate the {@link TextField}s raw input with;
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
			InputValidator<String> inputValidator, OptionPattern<? super TextField>... patterns) {
		return buildAndBind(ComponentFactory::buildTextField, property, inputValidator, patterns);
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
			Converter<String, PropertyType> converter, OptionPattern<? super TextField>... patterns) {
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
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link TextArea} to;
	 *            <b>not</b> allowed to be null.
	 * @param inputValidator
	 *            The validator to validate the {@link TextArea}s raw input with;
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
			InputValidator<String> inputValidator,
			OptionPattern<? super TextArea>... patterns) {
		return buildAndBind(ComponentFactory::buildTextArea, property, inputValidator, patterns);
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
			Converter<String, PropertyType> converter, OptionPattern<? super TextArea>... patterns) {
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
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link DateField} to;
	 *            <b>not</b> allowed to be null.
	 * @param inputValidator
	 *            The validator to validate the {@link DateField}s raw input with;
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
			InputValidator<LocalDate> inputValidator,
			OptionPattern<? super DateField>... patterns) {
		return buildAndBind(ComponentFactory::buildDateField, property, inputValidator, patterns);
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
			Converter<LocalDate, PropertyType> converter, OptionPattern<? super DateField>... patterns) {
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
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link DateTimeField}
	 *            to; <b>not</b> allowed to be null.
	 * @param inputValidator
	 *            The validator to validate the {@link DateTimeField}s raw input with;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link DateTimeField} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final DateTimeField bindDateTimeFieldForProperty(ModelProperty<ModelType, LocalDateTime> property,
			InputValidator<LocalDateTime> inputValidator,
			OptionPattern<? super DateTimeField>... patterns) {
		return buildAndBind(ComponentFactory::buildDateTimeField, property, inputValidator, patterns);
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
			ModelProperty<ModelType, PropertyType> property, Converter<LocalDateTime, PropertyType> converter,
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
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link CheckBox} to;
	 *            <b>not</b> allowed to be null.
	 * @param inputValidator
	 *            The validator to validate the {@link CheckBox}es raw input with;
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
			InputValidator<Boolean> inputValidator,
			OptionPattern<? super CheckBox>... patterns) {
		return buildAndBind(ComponentFactory::buildCheckBox, property, inputValidator, patterns);
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
			Converter<Boolean, PropertyType> converter, OptionPattern<? super CheckBox>... patterns) {
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
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link RadioButtonGroup}
	 *            to; <b>not</b> allowed to be null.
	 * @param inputValidator
	 *            The validator to validate the {@link RadioButtonGroup}s raw input with;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The {@link OptionPattern}s to apply to the new component; may be
	 *            null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return A new {@link RadioButtonGroup} instance, bound to the given
	 *         {@link ModelProperty}; never null
	 */
	@SafeVarargs
	public final <T> RadioButtonGroup<T> bindRadioButtonGroupForProperty(ModelProperty<ModelType, T> property,
			InputValidator<T> inputValidator,
			OptionPattern<? super RadioButtonGroup<?>>... patterns) {
		HasValueProvider<RadioButtonGroup<T>, T> provider = ComponentFactory::buildRadioButtonGroup;
		return buildAndBind(provider, property, inputValidator, patterns);
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
			ModelProperty<ModelType, PropertyType> property, Converter<T, PropertyType> converter,
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
	 *            The type of the property to bind.
	 * @param property
	 *            The {@link ModelProperty} to bind the new {@link ComboBox} to;
	 *            <b>not</b> allowed to be null.
	 * @param inputValidator
	 *            The validator to validate the {@link ComboBox}es raw input with;
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
			InputValidator<T> inputValidator,
			OptionPattern<? super ComboBox<?>>... patterns) {
		HasValueProvider<ComboBox<T>, T> provider = ComponentFactory::buildComboBox;
		return buildAndBind(provider, property, inputValidator, patterns);
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
			Converter<T, PropertyType> converter, OptionPattern<? super ComboBox<?>>... patterns) {
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