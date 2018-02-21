package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter;

import com.mantledillusion.injection.hura.annotation.Inject;
import com.mantledillusion.vaadin.cotton.testsuites.AbstractTestSuite;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.EventDispatcher;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.ReceivalLog;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.ViewA;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.ViewB;

public abstract class AbstractViewPresenterTestSuite extends AbstractTestSuite {

	@Inject
	public ViewA viewA;
	
	@Inject
	public ViewB viewB;

	@Inject
	public EventDispatcher dispatcher;

	@Inject(ReceivalLog.SINGLETON_ID)
	public ReceivalLog log;
}
