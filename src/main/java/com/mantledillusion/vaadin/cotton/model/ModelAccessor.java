package com.mantledillusion.vaadin.cotton.model;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.data.epiphy.ModelPropertyList;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Processor.Phase;
import com.mantledillusion.injection.hura.annotation.Inject;
import com.mantledillusion.injection.hura.annotation.Inject.InjectionMode;
import com.mantledillusion.injection.hura.annotation.Process;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.model.ValidationContext.ValidationErrorRegistry;
import com.vaadin.data.Binder;
import com.vaadin.data.Converter;
import com.vaadin.data.ErrorMessageProvider;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValueContext;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.Binder.BindingBuilder;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.server.Setter;

/**
 * A super type for an indexable proxy on a parent {@link ModelContainer}.
 * <p>
 * NOTE: Should be injected, since the {@link Injector} handles the instance's
 * life cycles.
 * <P>
 * Model accessing via this {@link ModelAccessor} will be forwarded to the
 * parent {@link ModelContainer}.
 *
 * @param <ModelType>
 *            The root type of the data model the {@link ModelAccessor}
 *            accesses.
 */
public abstract class ModelAccessor<ModelType> extends ModelBinder<ModelType> {

	@SuppressWarnings("rawtypes")
	private static final ValidationErrorRegistry EMPTY_ERROR_REGISTRY = new ValidationErrorRegistry<>();

	@Inject(value = IndexContext.SINGLETON_ID, injectionMode = InjectionMode.EXPLICIT)
	private IndexContext indexContext = IndexContext.EMPTY;

	private final ModelProxy<ModelType> parent;

	private Binder<ModelType> binder = new Binder<>();
	private final Map<ModelProperty<ModelType, ?>, Set<BindingReference>> boundFields = new IdentityHashMap<>();

	@SuppressWarnings("unchecked")
	private ValidationErrorRegistry<ModelType> validationErrors = EMPTY_ERROR_REGISTRY;

	/**
	 * Constructor.
	 * 
	 * @param parentContainer
	 *            The parent {@link ModelProxy} to use; might <b>not</b> be null.
	 */
	protected ModelAccessor(ModelProxy<ModelType> parentContainer) {
		if (parentContainer == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot create an accessor for a null parent container.");
		}
		this.parent = parentContainer;
		this.parent.register(this);
	}

	// ######################################################################################################################################
	// ############################################################ INTERNAL ################################################################
	// ######################################################################################################################################

	@Process(Phase.DESTROY)
	private void releaseReferences() {
		for (Set<BindingReference> references : this.boundFields.values()) {
			for (BindingReference reference : references) {
				reference.destroyBinding();
			}
		}
		this.boundFields.clear();
		this.binder.removeBean();
		this.binder = new Binder<>();

		this.parent.unregister(this);
	}

	// ######################################################################################################################################
	// ############################################################## INDEX #################################################################
	// ######################################################################################################################################

	@Override
	protected final IndexContext getIndexContext() {
		return this.indexContext;
	}

	// ######################################################################################################################################
	// ########################################################### MODEL CONTROL ############################################################
	// ######################################################################################################################################

	@Override
	public final boolean hasModel() {
		return this.binder.getBean() != null;
	}

	@Override
	public final ModelType getModel() {
		return this.parent.getModel();
	}

	@Override
	final void setModel(ModelType model) {
		this.binder.setBean(model);
		super.setModel(model);
	}

	@Override
	public final boolean isModelChanged() {
		return this.parent.isModelChanged();
	}

	@Override
	public final <TargetPropertyType> boolean isPropertyChanged(ModelProperty<ModelType, TargetPropertyType> property) {
		return this.parent.isPropertyChanged(property, this.indexContext);
	}

	@Override
	public final <TargetPropertyType> boolean isPropertyChanged(ModelProperty<ModelType, TargetPropertyType> property,
			IndexContext context) {
		return this.parent.isPropertyChanged(property, this.indexContext.union(context));
	}

	// ######################################################################################################################################
	// ###################################################### PROPERTIED MODEL ACCESS #######################################################
	// ######################################################################################################################################

	@Override
	public final <TargetPropertyType> TargetPropertyType getProperty(
			ModelProperty<ModelType, TargetPropertyType> property) {
		return this.parent.getProperty(property, this.indexContext);
	}

	@Override
	public final <TargetPropertyType> TargetPropertyType getProperty(
			ModelProperty<ModelType, TargetPropertyType> property, IndexContext indexContext) {
		return this.parent.getProperty(property, this.indexContext.union(indexContext));
	}

	@Override
	public final <TargetPropertyType> void setProperty(ModelProperty<ModelType, TargetPropertyType> property,
			TargetPropertyType value) {
		this.parent.setProperty(property, value, this.indexContext);
	}

	@Override
	public final <TargetPropertyType> void setProperty(ModelProperty<ModelType, TargetPropertyType> property,
			TargetPropertyType value, IndexContext indexContext) {
		this.parent.setProperty(property, value, this.indexContext.union(indexContext));
	}

	@Override
	public final <TargetPropertyType> void addProperty(ModelPropertyList<ModelType, TargetPropertyType> property,
			TargetPropertyType value) {
		this.parent.addProperty(property, value, this.indexContext);
	}

	@Override
	public final <TargetPropertyType> void addProperty(ModelPropertyList<ModelType, TargetPropertyType> property,
			TargetPropertyType value, IndexContext indexContext) {
		this.parent.addProperty(property, value, this.indexContext.union(indexContext));
	}

	@Override
	public final <TargetPropertyType> TargetPropertyType removeProperty(
			ModelPropertyList<ModelType, TargetPropertyType> property) {
		return this.parent.removeProperty(property, this.indexContext);
	}

	@Override
	public final <TargetPropertyType> TargetPropertyType removeProperty(
			ModelPropertyList<ModelType, TargetPropertyType> property, IndexContext indexContext) {
		return this.parent.removeProperty(property, this.indexContext.union(indexContext));
	}

	// ######################################################################################################################################
	// ############################################################## BINDING ###############################################################
	// ######################################################################################################################################

	private interface HasValueResetter<PropertyValueType> {

		void reset();
	}

	private final class BindingReference {
		private final HasValue<?> hasValue;
		private final HasValueResetter<?> resetter;

		private BindingReference(HasValue<?> hasValue, HasValueResetter<?> resetter) {
			this.hasValue = hasValue;
			this.resetter = resetter;
		}

		private void reset() {
			this.resetter.reset();
		}

		private void destroyBinding() {
			ModelAccessor.this.binder.removeBinding(this.hasValue);
		}
	}

	@Override
	public final <FieldType extends HasValue<PropertyValueType>, PropertyValueType> FieldType bindToProperty(
			FieldType field, ModelProperty<ModelType, PropertyValueType> property) {
		if (field == null) {
			throw new IllegalArgumentException("Cannot bind a null HasValue.");
		}

		BindingBuilder<ModelType, PropertyValueType> builder = this.binder.forField(field);
		HasValueResetter<PropertyValueType> resetter;
		if (field.getEmptyValue() != null) {
			builder = builder.withNullRepresentation(field.getEmptyValue());
			resetter = () -> {
				PropertyValueType propertyValue = ModelAccessor.this.getProperty(property);
				field.setValue(propertyValue == null ? field.getEmptyValue() : propertyValue);
			};
		} else {
			resetter = () -> field.setValue(ModelAccessor.this.getProperty(property));
		}

		bind(property, builder, resetter);
		return field;
	}

	@Override
	public final <FieldType extends HasValue<FieldValueType>, FieldValueType, PropertyValueType> FieldType bindToProperty(
			FieldType field, ModelProperty<ModelType, PropertyValueType> property,
			Converter<FieldValueType, PropertyValueType> converter) {
		if (field == null) {
			throw new IllegalArgumentException("Cannot bind a null HasValue.");
		} else if (converter == null) {
			throw new IllegalArgumentException("Cannot bind using a null converter.");
		}

		BindingBuilder<ModelType, FieldValueType> builder = this.binder.forField(field);
		HasValueResetter<PropertyValueType> resetter;
		if (field.getEmptyValue() != null) {
			builder = builder.withNullRepresentation(field.getEmptyValue());
			resetter = () -> {
				FieldValueType propertyValue = converter.convertToPresentation(ModelAccessor.this.getProperty(property),
						new ValueContext());
				field.setValue(propertyValue == null ? field.getEmptyValue() : propertyValue);
			};
		} else {
			resetter = () -> field.setValue(
					converter.convertToPresentation(ModelAccessor.this.getProperty(property), new ValueContext()));
		}

		bind(property, builder.withConverter(converter), resetter);
		return field;
	}

	private final <FieldValueType, PropertyValueType> void bind(ModelProperty<ModelType, PropertyValueType> property,
			BindingBuilder<ModelType, PropertyValueType> builder, HasValueResetter<PropertyValueType> setter) {
		if (property == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot bind a HasValue to a null property.");
		}
		builder.withValidator(new SerializablePredicate<PropertyValueType>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean test(PropertyValueType arg0) {
				return !ModelAccessor.this.validationErrors.errorMessages.containsKey(property);
			}
		}, new ErrorMessageProvider() {
			private static final long serialVersionUID = 1L;

			@Override
			public String apply(ValueContext context) {
				return StringUtils.join(ModelAccessor.this.validationErrors.errorMessages.get(property), ',');
			}
		}).bind(new ValueProvider<ModelType, PropertyValueType>() {
			private static final long serialVersionUID = 1L;

			@Override
			public PropertyValueType apply(ModelType source) {
				return ModelAccessor.this.getProperty(property);
			}
		}, new Setter<ModelType, PropertyValueType>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void accept(ModelType bean, PropertyValueType fieldvalue) {
				ModelAccessor.this.setProperty(property, fieldvalue);
			}
		});

		if (!this.boundFields.containsKey(property)) {
			this.boundFields.put(property, new HashSet<>());
		}
		this.boundFields.get(property).add(new BindingReference(builder.getField(), setter));
	}

	// ######################################################################################################################################
	// ############################################################## UPDATE ################################################################
	// ######################################################################################################################################

	final <PropertyValueType> void updatePropertyBoundFields(IndexContext context,
			Set<ModelProperty<ModelType, ?>> properties) {
		if (context.contains(this.indexContext)) {
			for (ModelProperty<ModelType, ?> property : properties) {
				if (this.boundFields.containsKey(property)) {
					for (BindingReference setter : this.boundFields.get(property)) {
						setter.reset();
					}
				}
			}
			for (ModelAccessor<ModelType> child : getChildren()) {
				child.updatePropertyBoundFields(context, properties);
			}
		}
	}

	final void updatePropertyIndex(ModelProperty<ModelType, ?> property, int baseIndex, int modification) {
		this.indexContext = this.indexContext.update(property, baseIndex, modification);
		for (ModelAccessor<ModelType> child : getChildren()) {
			child.updatePropertyIndex(property, baseIndex, modification);
		}
	}

	// ######################################################################################################################################
	// ############################################################ VALIDATION ##############################################################
	// ######################################################################################################################################

	@Override
	@SuppressWarnings("unchecked")
	final void applyErrors(ValidationErrorRegistry<ModelType> errorRegistry) {
		this.validationErrors = errorRegistry;
		this.binder.validate();
		this.validationErrors = EMPTY_ERROR_REGISTRY;
		for (ModelAccessor<ModelType> child : getChildren()) {
			child.applyErrors(errorRegistry);
		}
	}

	@Override
	final void clearErrors() {
		this.binder.validate();
		for (ModelAccessor<ModelType> child : getChildren()) {
			child.clearErrors();
		}
	}

	// ######################################################################################################################################
	// ############################################################ PERSISTING ##############################################################
	// ######################################################################################################################################

	@Override
	public final ModelType persist() {
		return this.parent.persist(this.indexContext);
	}

	@Override
	public final ModelType persist(IndexContext context) {
		return this.parent.persist(context);
	}
}
