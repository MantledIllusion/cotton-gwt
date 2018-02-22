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

	@Inject(value = IndexContext.SINGLETON_ID, injectionMode = InjectionMode.EXPLICIT)
	private IndexContext indexContext = IndexContext.EMPTY;

	private final ModelProxy<ModelType> parent;

	private Binder<ModelType> binder = new Binder<>();
	private final Map<ModelProperty<ModelType, ?>, Set<PropertyResetter<?>>> boundFields = new IdentityHashMap<>();
	private ValidationErrorRegistry<ModelType> validationErrors = new ValidationErrorRegistry<ModelType>();

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
		this.binder.removeBean();
		this.binder = new Binder<>();
		this.validationErrors = new ValidationErrorRegistry<ModelType>();
		this.boundFields.clear();
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

	private interface PropertyResetter<PropertyValueType> {

		void reset();
	}

	@Override
	final <PropertyType> void bind(HasValue<PropertyType> field, ModelProperty<ModelType, PropertyType> property) {
		bind(property, this.binder.forField(field), () -> field.setValue(ModelAccessor.this.getProperty(property)));
	}

	@Override
	final <FieldValueType, PropertyValueType> void bind(HasValue<FieldValueType> field,
			ModelProperty<ModelType, PropertyValueType> property,
			Converter<FieldValueType, PropertyValueType> converter) {
		bind(property, this.binder.forField(field).withConverter(converter), () -> field.setValue(
				converter.convertToPresentation(ModelAccessor.this.getProperty(property), new ValueContext())));
	}

	private final <FieldValueType, PropertyValueType> void bind(ModelProperty<ModelType, PropertyValueType> property,
			BindingBuilder<ModelType, PropertyValueType> builder, PropertyResetter<PropertyValueType> setter) {
		if (property == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot bind a component for a null property.");
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
		this.boundFields.get(property).add(setter);
	}

	// ######################################################################################################################################
	// ############################################################## UPDATE ################################################################
	// ######################################################################################################################################

	@SuppressWarnings("unchecked")
	final <PropertyValueType> void updatePropertyBoundFields(IndexContext context,
			Set<ModelProperty<ModelType, ?>> properties) {
		if (context.contains(this.indexContext)) {
			for (ModelProperty<ModelType, ?> property : properties) {
				if (this.boundFields.containsKey(property)) {
					for (PropertyResetter<?> setter : this.boundFields.get(property)) {
						((PropertyResetter<PropertyValueType>) setter).reset();
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
	final void applyErrors(ValidationErrorRegistry<ModelType> errorRegistry) {
		this.validationErrors = errorRegistry;
		this.binder.validate();
		this.validationErrors.errorMessages.clear();
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
