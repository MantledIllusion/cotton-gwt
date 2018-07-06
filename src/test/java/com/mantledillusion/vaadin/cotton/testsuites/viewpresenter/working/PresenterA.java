package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working;

import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.PropertiedEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerAEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerAToTriggerBToTriggerAEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerBToTriggerAEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.UniversalEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.ViewAComponentFiredEvent;
import com.mantledillusion.vaadin.cotton.viewpresenter.Listen;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe.EventProperty;
import com.vaadin.ui.Button.ClickEvent;

public class PresenterA extends AbstractLoggingPresenter<ViewA> {
	
	@Listen(ViewA.COMPONENT_ID)
	private void listen(ClickEvent event) {
		logAndDispatch(new ViewAComponentFiredEvent());
	}
	
	@Subscribe
	private void triggerBToTriggerA(TriggerAToTriggerBToTriggerAEvent event) {
		log(event);
		logAndDispatch(new TriggerBToTriggerAEvent());
	}
	
	@Subscribe
	private void subscribeForA(TriggerAEvent event) {
		log(event);
	}
	
	@Subscribe(@EventProperty(key=PropertiedEvent.SUBSCRIBER_KEY, value="A"))
	private void subscribeForA(PropertiedEvent event) {
		log(event);
	}

	@Subscribe(isSelfObservant=false)
	public void subscribeForA(UniversalEvent event) {
		log(event);
	}
}
