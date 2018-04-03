package com.mantledillusion.vaadin.cotton.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.data.epiphy.ModelPropertyList;
import com.mantledillusion.data.epiphy.index.PropertyIndex;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Processor.Phase;
import com.mantledillusion.injection.hura.annotation.Construct;
import com.mantledillusion.injection.hura.annotation.Process;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

/**
 * Model container that enables model browsing via {@link ModelProperty} and can
 * be used by {@link ModelAccessor}s as model data source.
 * <p>
 * NOTE: Should be injected, since the {@link Injector} handles the instance's
 * life cycles.
 * <p>
 * {@link ModelContainer}s are always unindexed; for accessing
 * {@link ModelPropertyList}s or their children in this container's model
 * instance, either provide an {@link IndexContext} to the containers several
 * data {@link Method}s or create an {@link ModelAccessor} with this container
 * as a parent that is able to index and proxy calls to is data {@link Method}s.
 *
 * @param <ModelType>
 *            The root type of the data model the {@link ModelContainer} can
 *            contain.
 */
public final class ModelContainer<ModelType> extends ModelProxy<ModelType> {

	public static final String DEFAULT_SINGLETON_ID = "_containerSingletonId";

	private final Map<ModelProperty<ModelType, ?>, ModelPersistor<ModelType, ?>> persistors = new IdentityHashMap<>();

	private ModelType dataModel;
	private final Map<ModelProperty<ModelType, ?>, Set<IndexContext>> changeLog = new IdentityHashMap<>();

	@Construct
	private ModelContainer() {
	}

	// ######################################################################################################################################
	// ############################################################ INTERNAL ################################################################
	// ######################################################################################################################################

	void register(ModelPersistor<ModelType, ?> childPersistor) {
		if (this.persistors.containsKey(childPersistor.getProperty())) {
			throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
					"Cannot register a persistor of the type " + childPersistor.getClass().getSimpleName()
							+ " for the property " + childPersistor.getProperty() + "; an persistor of the type "
							+ this.persistors.get(childPersistor.getProperty())
							+ " is already registered for that property.");
		}
		this.persistors.put(childPersistor.getProperty(), childPersistor);
	}

	void unregister(ModelPersistor<ModelType, ?> childPersistor) {
		if (this.persistors.get(childPersistor.getProperty()) == childPersistor) {
			this.persistors.remove(childPersistor.getProperty());
		}
	}

	@Process(Phase.DESTROY)
	private void releaseReferences() {
		this.persistors.clear();
	}

	// ######################################################################################################################################
	// ############################################################## INDEX #################################################################
	// ######################################################################################################################################

	@Override
	public final IndexContext getIndexContext() {
		return IndexContext.EMPTY;
	}

	// ######################################################################################################################################
	// ########################################################### MODEL CONTROL ############################################################
	// ######################################################################################################################################

	@Override
	public final boolean hasModel() {
		return this.dataModel != null;
	}

	/**
	 * Sets this {@link ModelContainer}'s model to null.
	 */
	public final void clearModel() {
		setModel(null);
	}

	@Override
	public final ModelType getModel() {
		return this.dataModel;
	}

	/**
	 * Sets this {@link ModelContainer}'s model to the given instance.
	 * <P>
	 * Also resets the property change log.
	 * 
	 * @param model
	 *            The model to set; might be null.
	 */
	@Override
	public final void setModel(ModelType model) {
		this.dataModel = model;
		this.changeLog.clear();
		super.setModel(model);
	}

	@Override
	public final boolean isModelChanged() {
		return !this.changeLog.isEmpty();
	}

	/**
	 * Clears the log of changes to the current model since the last clearing or the
	 * moment the model has been applied to the container.
	 */
	public final void resetPropertyChangeLog() {
		this.changeLog.clear();
	}

	@Override
	public final <PropertyType> boolean isPropertyChanged(ModelProperty<ModelType, PropertyType> property) {
		return isPropertyChanged(property, null);
	}

	@Override
	public final <PropertyType> boolean isPropertyChanged(ModelProperty<ModelType, PropertyType> property,
			IndexContext indexContext) {
		if (property == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot return any value for property null.");
		} else {
			IndexContext reducedContext;
			if (indexContext != null) {
				reducedContext = indexContext.intersection(property.getIndices());
			} else {
				reducedContext = IndexContext.EMPTY;
			}

			return isSinglePropertyChanged(property, reducedContext) || property.getAllChildren().stream()
					.anyMatch(child -> isSinglePropertyChanged(child, reducedContext));
		}
	}

	private <PropertyType> boolean isSinglePropertyChanged(ModelProperty<ModelType, PropertyType> property,
			IndexContext indexContext) {
		if (this.changeLog.containsKey(property)) {
			for (IndexContext changedContext : this.changeLog.get(property)) {
				if (changedContext.contains(indexContext)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public final <PropertyType> boolean exists(ModelProperty<ModelType, PropertyType> property) {
		return exists(property, IndexContext.EMPTY);
	}

	@Override
	public final <PropertyType> boolean exists(ModelProperty<ModelType, PropertyType> property, IndexContext context) {
		return property.exists(this.dataModel, context);
	}

	// ######################################################################################################################################
	// ###################################################### PROPERTIED MODEL ACCESS #######################################################
	// ######################################################################################################################################

	@Override
	public final <PropertyType> PropertyType getProperty(ModelProperty<ModelType, PropertyType> property) {
		return getProperty(property, IndexContext.EMPTY);
	}

	@Override
	public final <TargetPropertyType> TargetPropertyType getProperty(
			ModelProperty<ModelType, TargetPropertyType> property, IndexContext indexContext) {
		indexContext = ObjectUtils.defaultIfNull(indexContext, IndexContext.EMPTY);
		return property.get(this.dataModel, indexContext, true);
	}

	@Override
	public final <TargetPropertyType> void setProperty(ModelProperty<ModelType, TargetPropertyType> property,
			TargetPropertyType value) {
		setProperty(property, value, IndexContext.EMPTY);
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <TargetPropertyType> void setProperty(ModelProperty<ModelType, TargetPropertyType> property,
			TargetPropertyType value, IndexContext indexContext) {
		if (property.isRoot()) {
			setModel((ModelType) value);
		} else {
			indexContext = ObjectUtils.defaultIfNull(indexContext, IndexContext.EMPTY);
			property.set(this.dataModel, value, indexContext);
		}

		registerPropertyChange(property, indexContext);

		if (!property.isRoot()) {
			updatePropertyBoundFieldsOfChildren(property, indexContext);
		}
	}

	@Override
	public <TargetPropertyType> void addProperty(ModelPropertyList<ModelType, TargetPropertyType> property,
			TargetPropertyType value) {
		addProperty(property, value, IndexContext.EMPTY);
	}

	@Override
	public <TargetPropertyType> void addProperty(ModelPropertyList<ModelType, TargetPropertyType> property,
			TargetPropertyType value, IndexContext indexContext) {
		indexContext = ObjectUtils.defaultIfNull(indexContext, IndexContext.EMPTY);
		property.add(this.dataModel, value, indexContext);

		registerPropertyChange(property, indexContext);

		updatePropertyIndexOfChildren(property, indexContext.indexOf(property), +1);

		updatePropertyBoundFieldsOfChildren(property.getParent(),
				indexContext.intersection(Collections.singleton(property)));
	}

	@Override
	public <TargetPropertyType> TargetPropertyType removeProperty(
			ModelPropertyList<ModelType, TargetPropertyType> property) {
		return removeProperty(property, IndexContext.EMPTY);
	}

	@Override
	public <TargetPropertyType> TargetPropertyType removeProperty(
			ModelPropertyList<ModelType, TargetPropertyType> property, IndexContext indexContext) {
		indexContext = ObjectUtils.defaultIfNull(indexContext, IndexContext.EMPTY);
		TargetPropertyType value = property.remove(this.dataModel, indexContext);

		registerPropertyChange(property, indexContext);

		updatePropertyIndexOfChildren(property, indexContext.indexOf(property), -1);

		updatePropertyBoundFieldsOfChildren(property.getParent(),
				indexContext.intersection(Collections.singleton(property)));

		return value;
	}

	private void registerPropertyChange(ModelProperty<ModelType, ?> property, IndexContext indexContext) {
		if (!this.changeLog.containsKey(property)) {
			this.changeLog.put(property, new HashSet<>());
		}
		this.changeLog.get(property).add(indexContext.intersection(property.getIndices()));
	}

	private <PropertyType> void updatePropertyIndexOfChildren(ModelProperty<ModelType, PropertyType> property,
			int baseIndex, int modification) {
		for (ModelAccessor<ModelType> child : getChildren()) {
			child.updatePropertyIndex(property, baseIndex, modification);
		}
	}

	private <PropertyType> void updatePropertyBoundFieldsOfChildren(ModelProperty<ModelType, PropertyType> property,
			IndexContext indexContext) {
		Set<ModelProperty<ModelType, ?>> changedProperties = SetUtils.union(Collections.singleton(property),
				property.getAllChildren());
		for (ModelAccessor<ModelType> child : getChildren()) {
			child.updatePropertyBoundFields(indexContext, changedProperties);
		}
	}

	// ######################################################################################################################################
	// ############################################################ VALIDATION ##############################################################
	// ######################################################################################################################################

	@Override
	final void gatherPreevalutationErrors(ValidationErrorRegistry<ModelType> errorRegistry) {
		for (ModelAccessor<ModelType> childAccessor : getChildren()) {
			childAccessor.gatherPreevalutationErrors(errorRegistry);
		}
	}
	
	@Override
	final void applyErrors(ValidationErrorRegistry<ModelType> errorRegistry) {
		for (ModelAccessor<ModelType> childAccessor : getChildren()) {
			childAccessor.applyErrors(errorRegistry);
		}
	}

	@Override
	final void clearErrors() {
		for (ModelAccessor<ModelType> childAccessor : getChildren()) {
			childAccessor.clearErrors();
		}
	}

	// ######################################################################################################################################
	// ############################################################ PERSISTING ##############################################################
	// ######################################################################################################################################

	@Override
	public final ModelType persist() {
		return persist(null);
	}

	@Override
	public final ModelType persist(IndexContext context) {

		context = ObjectUtils.defaultIfNull(context, IndexContext.EMPTY);

		// EXTRACT THE PROPERTIES (OR THEIR PARENTS) FROM THE PROPERTIES IN THE CHANGE LOG THAT HAVE A PERSISTOR REGISTERED
		Map<ModelProperty<ModelType, ?>, Set<IndexContext>> persistableProperties = new IdentityHashMap<>();
		for (ModelProperty<ModelType, ?> baseProperty : this.changeLog.keySet()) {

			// FIND THE (PARENT) PROPERTY WHOSE PERSISTOR IS AVAILABLE
			ModelProperty<ModelType, ?> persistableProperty = baseProperty;
			while (!this.persistors.containsKey(persistableProperty)) {
				if (persistableProperty == null) {
					throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
							"Unable to persist all changes; there is no " + ModelPersistor.class.getSimpleName()
									+ " registered for the changed property " + baseProperty
									+ " or one of its parents.");
				}
				persistableProperty = persistableProperty.getParent();
			}

			// FIND THE CHANGES THAT ARE INCLUDED IN THE TARGET CONTEXT
			Set<IndexContext> propertyContexts = new HashSet<>();
			IndexContext reducedTargetContext = context.intersection(baseProperty.getContext());
			for (IndexContext propertyContext : this.changeLog.get(baseProperty)) {
				IndexContext reducedPropertyContext = propertyContext.intersection(baseProperty.getContext());
				if (reducedPropertyContext.contains(reducedTargetContext)) {
					propertyContexts.add(reducedPropertyContext);
				}
			}

			if (!propertyContexts.isEmpty()) {
				if (!persistableProperties.containsKey(persistableProperty)) {
					persistableProperties.put(persistableProperty, propertyContexts);
				} else {
					persistableProperties.get(persistableProperty).addAll(propertyContexts);
				}
			}
		}

		// REMOVE THOSE PROPERTIES WHOSE PERSISTING WILL BE INCLUDED IN THE PERSISTING OF PARENTS
		for (ModelProperty<ModelType, ?> parent : new HashSet<>(persistableProperties.keySet())) {
			for (ModelProperty<ModelType, ?> child : parent.getAllChildren()) {
				if (parent != child && persistableProperties.containsKey(child)) {
					Iterator<IndexContext> iter = persistableProperties.get(child).iterator();
					IndexContext childContext;
					while (iter.hasNext()) {
						childContext = iter.next();
						for (IndexContext parentContext : persistableProperties.get(parent)) {
							if (childContext.contains(parentContext)) {
								iter.remove();
								break;
							}
						}
					}
					if (persistableProperties.get(child).isEmpty()) {
						persistableProperties.remove(child);
					}
				}
			}
		}

		// TRIGGER PERSISTING
		for (ModelProperty<ModelType, ?> property : persistableProperties.keySet()) {
			for (IndexContext current : persistableProperties.get(property)) {
				persistProperty(property, current);
			}
		}

		return this.dataModel;
	}

	final <TargetPropertyType> List<TargetPropertyType> persistProperty(
			ModelProperty<ModelType, TargetPropertyType> property, IndexContext context) {
		List<TargetPropertyType> persistedInstances = new ArrayList<>();
		for (IndexContext possibleContext : determinePossiblePropertyContexts(property, context)) {
			TargetPropertyType instance = getProperty(property, possibleContext);
			if (instance != null) {
				@SuppressWarnings("unchecked")
				ModelPersistor<ModelType, TargetPropertyType> persistor = (ModelPersistor<ModelType, TargetPropertyType>) this.persistors
						.get(property);

				try {
					persistedInstances.add(persistor.persistInstance(instance));
					removeFromChangeLog(property, possibleContext);
					property.getAllChildren().stream().forEach(child -> removeFromChangeLog(child, possibleContext));
				} catch (Throwable t) {
					throw new WebException(HttpErrorCodes.HTTP500_INTERNAL_SERVER_ERROR,
							"The persistor " + persistor.getClass().getSimpleName()
									+ " threw an exception while persisting the " + instance.getClass().getSimpleName()
									+ " instance " + instance + " at InjectableIndexContext " + possibleContext + ".",
							t);
				}
			}
		}
		return persistedInstances;
	}

	/*
	 * Determine all contexts that are possible in reaching instances of the given
	 * property on the basis of the given context
	 */
	@SuppressWarnings("unchecked")
	private final Set<IndexContext> determinePossiblePropertyContexts(ModelProperty<ModelType, ?> property,
			IndexContext context) {
		Set<IndexContext> contexts = new HashSet<>(Arrays.asList(context));
		for (ModelProperty<ModelType, ?> pathProperty : property.getPath()) {
			if (pathProperty.isList() && !context.contains(pathProperty)) {
				Set<IndexContext> newContexts = new HashSet<>();
				for (IndexContext existingContext : contexts) {
					int count = ((Collection<?>) getProperty(pathProperty, existingContext)).size();
					for (int i = 0; i < count; i++) {
						newContexts.add(existingContext
								.union(PropertyIndex.of((ModelPropertyList<ModelType, ?>) pathProperty, i)));
					}
				}
				contexts = newContexts;
			}
		}
		return contexts;
	}

	private final void removeFromChangeLog(ModelProperty<ModelType, ?> property, IndexContext context) {
		if (this.changeLog.containsKey(property)) {
			Iterator<IndexContext> iter = this.changeLog.get(property).iterator();
			IndexContext childContext;
			while (iter.hasNext()) {
				childContext = iter.next();
				if (childContext.contains(context)) {
					iter.remove();
				}
			}
			if (this.changeLog.get(property).isEmpty()) {
				this.changeLog.remove(property);
			}
		}
	}
}
