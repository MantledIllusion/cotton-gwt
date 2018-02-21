package com.mantledillusion.vaadin.cotton.testsuites.model;

import com.mantledillusion.injection.hura.annotation.Inject;
import com.mantledillusion.vaadin.cotton.model.IndexContext;
import com.mantledillusion.vaadin.cotton.model.ModelContainer;
import com.mantledillusion.vaadin.cotton.testsuites.AbstractTestSuite;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.Model;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.TestModelAccessor;

public abstract class AbstractModelTestSuite extends AbstractTestSuite {

	public static final String CONTAINER_SINGLETON_ID = "modelContainer";

	@Inject(CONTAINER_SINGLETON_ID)
	public ModelContainer<Model> container;

	public TestModelAccessor createIndexedAccessor(IndexContext indexContext) {
		return injectInSuiteContext(TestModelAccessor.class, indexContext);
	}
}
