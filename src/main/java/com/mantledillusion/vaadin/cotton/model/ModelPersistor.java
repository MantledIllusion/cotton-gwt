package com.mantledillusion.vaadin.cotton.model;

import java.lang.reflect.Constructor;
import java.util.List;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.injection.hura.Processor.Phase;
import com.mantledillusion.injection.hura.annotation.Process;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

/**
 * A super type for an indexable persistence unit of a single
 * {@link ModelProperty}.
 * <P>
 * Persisting via this {@link ModelPersistor} will be forwarded to the parent
 * {@link ModelContainer}.
 *
 * @param <ModelType>
 *            The root type of the data model the {@link ModelPersistor}
 *            persists.
 * @param <PropertyType>
 *            The type this {@link ModelPersistor}s property points to.
 */
public abstract class ModelPersistor<ModelType, PropertyType> {

	private final ModelContainer<ModelType> parentContainer;
	private final ModelProperty<ModelType, PropertyType> property;

	/**
	 * {@link Constructor}.
	 * 
	 * @param parentContainer
	 *            The parent {@link ModelContainer} to use; might <b>not</b> be
	 *            null.
	 * @param property
	 *            The property this persistor is able to persist; might <b>not</b>
	 *            be null.
	 */
	protected ModelPersistor(ModelContainer<ModelType> parentContainer,
			ModelProperty<ModelType, PropertyType> property) {
		if (parentContainer == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot create a persistor for a null parent container.");
		} else if (property == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot create an persistor for a null target property.");
		}
		this.parentContainer = parentContainer;
		this.property = property;

		this.parentContainer.register(this);
	}

	// ######################################################################################################################################
	// ############################################################ INTERNAL ################################################################
	// ######################################################################################################################################

	@Process(Phase.DESTROY)
	private void releaseReferences() {
		this.parentContainer.unregister(this);
	}

	// ######################################################################################################################################
	// ########################################################### PERSISTING ###############################################################
	// ######################################################################################################################################

	/**
	 * Returns the {@link ModelProperty} this {@link ModelPersistor} is able to
	 * persist.
	 * 
	 * @return The property this {@link ModelPersistor} has registered itself on its
	 *         parent {@link ModelContainer} to persist; never null
	 */
	public final ModelProperty<ModelType, PropertyType> getProperty() {
		return this.property;
	}

	/**
	 * Triggers the parent {@link ModelContainer} to persist all changes of
	 * instances this {@link ModelPersistor}'s property points to.
	 * <P>
	 * Note that if the path from the property model's root to this
	 * {@link ModelPersistor}'s property is indexed, the used index context has an
	 * impact on the execution's result.
	 * <P>
	 * For determining the properties to persist, an empty index context is used.
	 * <P>
	 * Since the used index context can point to 0-n instances for this
	 * {@link ModelPersistor}'s property, there are also 0-n persisting executions
	 * to be expected, which is the reason why this method returns a list of
	 * results, not one.
	 * 
	 * @return The results of the persisting processes; never null, might be empty
	 *         if there are none
	 */
	public final List<PropertyType> persist() {
		return this.parentContainer.persistProperty(this.property, IndexContext.EMPTY);
	}

	/**
	 * Triggers the parent {@link ModelContainer} to persist all changes of
	 * instances this {@link ModelPersistor}'s property points to.
	 * <P>
	 * Note that if the path from the property model's root to this
	 * {@link ModelPersistor}'s property is indexed, the used index context has an
	 * impact on the execution's result.
	 * <P>
	 * For determining the properties to persist, the given index context is used.
	 * <P>
	 * Since the used index context can point to 0-n instances for this
	 * {@link ModelPersistor}'s property, there are also 0-n persisting executions
	 * to be expected, which is the reason why this method returns a list of
	 * results, not one.
	 * 
	 * @param context
	 *            The {@link IndexContext} used to determine the instances to
	 *            persist.
	 * @return The results of the persisting processes; never null, might be empty
	 *         if there are none.
	 */
	public final List<PropertyType> persist(IndexContext context) {
		return this.parentContainer.persistProperty(this.property, context);
	}

	/**
	 * Persists the given instance.
	 * <P>
	 * Will be called for every instance of the type this {@link ModelPersistor}'s
	 * property points to when its parent {@link ModelContainer} persists its model.
	 * <P>
	 * Note that a {@link ModelPersistor} is excepted to persist the instance its
	 * property points to <b>and all of its sub instances of possible sub
	 * properties</b>. If it does not do that and such a sub instance has pending
	 * changes, these pending changes will be cleared although the data is still not
	 * persisted.
	 * 
	 * @param propertyInstance
	 *            The instance to persist.
	 * @return The persisted property instance, since it might have changed from the
	 *         given one (for example, it might carry database entity IDs after
	 *         persisting). Will be used to update this {@link ModelPersistor}'s
	 *         parent {@link ModelContainer}'s model.
	 * @throws Throwable
	 *             Any {@link Throwable} the persisting process might throw. If it
	 *             is no {@link WebException}, it will be wrapped into one.
	 */
	protected abstract PropertyType persistInstance(PropertyType propertyInstance) throws Throwable;
}
