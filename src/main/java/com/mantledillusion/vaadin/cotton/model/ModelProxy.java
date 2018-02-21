package com.mantledillusion.vaadin.cotton.model;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;

import com.mantledillusion.injection.hura.Processor.Phase;
import com.mantledillusion.injection.hura.annotation.Process;

/**
 * Framework internal type <b>(DO NOT USE!)</b> for types that are able to proxy
 * {@link IModelProperty}ed model access of child
 * {@link ModelAccessor}s.
 *
 * @param <ModelType>
 *            The root type of the data model the
 *            {@link ModelValidationHandler} is able to persist.
 */
abstract class ModelProxy<ModelType> extends ModelValidationHandler<ModelType> {

	private final IdentityHashMap<ModelAccessor<ModelType>, Void> children = new IdentityHashMap<>();

	public ModelProxy() {
	}

	// ######################################################################################################################################
	// ############################################################ INTERNAL ################################################################
	// ######################################################################################################################################

	final void register(ModelAccessor<ModelType> childAccessor) {
		this.children.put(childAccessor, null);
		childAccessor.setModel(getModel());
	}

	final void unregister(ModelAccessor<ModelType> childAccessor) {
		this.children.remove(childAccessor);
	}

	final Collection<ModelAccessor<ModelType>> getChildren() {
		return Collections.unmodifiableCollection(this.children.keySet());
	}

	@Process(Phase.DESTROY)
	private void releaseReferences() {
		this.children.clear();
	}

	// ######################################################################################################################################
	// ############################################################## INDEX #################################################################
	// ######################################################################################################################################

	/**
	 * Returns the {@link IndexContext} of this {@link ModelProxy}
	 * implementation's instance.
	 * 
	 * @return The {@link IndexContext} of this proxy; never null
	 */
	abstract IndexContext getIndexContext();

	// ######################################################################################################################################
	// ########################################################### MODEL CONTROL ############################################################
	// ######################################################################################################################################

	void setModel(ModelType model) {
		for (ModelAccessor<ModelType> child : getChildren()) {
			child.setModel(model);
		}
	}
}
