package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working;

import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.PropertiedEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerAEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerBToTriggerAEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerBToTriggerUniversalEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.UniversalEvent;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe.EventProperty;

public class PresenterB extends AbstractLoggingPresenter<ViewB> {

	@Subscribe
	public void triggerA(TriggerBToTriggerAEvent event) {
		log(event);
		logAndDispatch(new TriggerAEvent());
	}
	
	@Subscribe(@EventProperty(key=PropertiedEvent.SUBSCRIBER_KEY, value="B"))
	private void subscribeForB(PropertiedEvent event) {
		log(event);
	}
	
	@Subscribe
	public void triggerUniversal(TriggerBToTriggerUniversalEvent event) {
		log(event);
		logAndDispatch(new UniversalEvent());
	}

	@Subscribe(isSelfObservant=false)
	public void subscribeForB(UniversalEvent event) {
		log(event);
	}
}
