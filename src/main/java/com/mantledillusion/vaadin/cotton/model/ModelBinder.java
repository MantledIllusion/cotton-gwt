package com.mantledillusion.vaadin.cotton.model;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.data.epiphy.ModelPropertyList;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory.OptionPattern;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
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
		ModelBinder.this.bind(comp, property);
		return comp;
	}

	abstract <PropertyType> void bind(HasValue<PropertyType> field, ModelProperty<ModelType, PropertyType> property);

	private <FieldType extends Component & HasValue<FieldValueType>, FieldValueType, PropertyType> FieldType buildAndBind(
			HasValueProvider<FieldType, FieldValueType> provider, ModelProperty<ModelType, PropertyType> property,
			Converter<FieldValueType, PropertyType> converter,
			@SuppressWarnings("unchecked") OptionPattern<? super FieldType>... patterns) {
		if (property == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot bind a component for a null property.");
		} else if (converter == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot bind a component to a differently typed property using a null converter.");
		}
		FieldType comp = provider.build(patterns);
		ModelBinder.this.bind(comp, property, converter);
		return comp;
	}

	abstract <FieldValueType, PropertyValueType> void bind(HasValue<FieldValueType> field,
			ModelProperty<ModelType, PropertyValueType> property,
			Converter<FieldValueType, PropertyValueType> converter);

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
	public final TextField buildTextFieldForProperty(ModelProperty<ModelType, String> property,
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
	public final <PropertyType> TextField buildTextFieldForProperty(ModelProperty<ModelType, PropertyType> property,
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
	public final TextArea buildTextAreaForProperty(ModelProperty<ModelType, String> property,
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
	public final <PropertyType> TextArea buildTextAreaForProperty(ModelProperty<ModelType, PropertyType> property,
			Converter<String, PropertyType> converter, OptionPattern<? super TextArea>... patterns) {
		return buildAndBind(ComponentFactory::buildTextArea, property, converter, patterns);
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
	public final CheckBox buildCheckBoxForProperty(ModelProperty<ModelType, Boolean> property,
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
	public final <PropertyType> CheckBox buildCheckBoxForProperty(ModelProperty<ModelType, PropertyType> property,
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
	public final <T> RadioButtonGroup<T> buildRadioButtonGroupForProperty(ModelProperty<ModelType, T> property,
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
	public final <T, PropertyType> RadioButtonGroup<T> buildRadioButtonGroupForProperty(
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
	public final <T> ComboBox<T> buildComboBoxForProperty(ModelProperty<ModelType, T> property,
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
	public final <T, PropertyType> ComboBox<T> buildComboBoxForProperty(ModelProperty<ModelType, PropertyType> property,
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
	public final <T> BindableGrid<T, ModelType> buildGridForProperty(
			ModelPropertyList<ModelType, T> property, OptionPattern<? super BindableGrid<?, ?>>... patterns) {
		BindableGrid<T, ModelType> table = new BindableGrid<T, ModelType>(this, property);
		bind(table.getBindable(), property);
		ComponentFactory.apply(table, patterns);
		return table;
	}
}