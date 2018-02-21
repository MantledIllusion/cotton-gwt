package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working;

import com.mantledillusion.injection.hura.annotation.Inject;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

public class AbstractLoggingPresenter<T extends View> extends Presenter<T> {

	@Inject(ReceivalLog.SINGLETON_ID)
	private ReceivalLog log;

	protected void log(BusEvent event) {
		log.received(this, event);
	}
	
	protected void logAndDispatch(BusEvent event) {
		log.dispatch(this, event);
		dispatch(event);
	}
}
