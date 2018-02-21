package com.mantledillusion.vaadin.cotton.testsuites;

import com.mantledillusion.injection.hura.Blueprint;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.annotation.Inject;
import com.mantledillusion.vaadin.cotton.model.IndexContext;

public class AbstractTestSuite {
	
	@Inject
	private Injector injector;
	
	public <T> T injectInSuiteContext(Class<T> type) {
		return this.injector.instantiate(type);
	}
	
	public <T> T injectInSuiteContext(TypedBlueprint<T> blueprint) {
		return this.injector.instantiate(blueprint);
	}
	
	public <T> T injectInSuiteContext(Class<T> type, IndexContext context) {
		return this.injector.instantiate(Blueprint.of(type, context.asSingleton()));
	}
	
	public void destroyInSuiteContext(Object wiredInstance) {
		this.injector.destroy(wiredInstance);
	}
}
