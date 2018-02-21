package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber.BusEvent;

public class PropertiedEvent extends BusEvent {
	
	public static final String SUBSCRIBER_KEY = "interestedSubscriber";
	
	public PropertiedEvent(String propertyValue) {
		super(SUBSCRIBER_KEY, propertyValue);
	}
}
