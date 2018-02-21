package com.mantledillusion.vaadin.cotton.testsuites.model.working;

import com.mantledillusion.vaadin.cotton.model.ModelAccessor;
import com.mantledillusion.vaadin.cotton.model.ModelContainer;
import com.mantledillusion.vaadin.cotton.testsuites.model.AbstractModelTestSuite;
import com.mantledillusion.injection.hura.annotation.Construct;
import com.mantledillusion.injection.hura.annotation.Inject;

public final class TestModelAccessor extends ModelAccessor<Model> {

	@Construct
	private TestModelAccessor(@Inject(AbstractModelTestSuite.CONTAINER_SINGLETON_ID) ModelContainer<Model> parentContainer) {
		super(parentContainer);
	}
}