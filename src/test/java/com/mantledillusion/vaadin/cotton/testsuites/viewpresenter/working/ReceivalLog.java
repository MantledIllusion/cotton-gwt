package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;

public class ReceivalLog extends EventBusSubscriber {

	public static final String SINGLETON_ID = "eventLog";
	
	private final List<Triple<Boolean, EventBusSubscriber, BusEvent>> events = new ArrayList<>();
	
	public void dispatch(EventBusSubscriber subscriber, BusEvent event) {
		this.events.add(Triple.of(Boolean.TRUE, subscriber, event));
	}
	
	public void received(EventBusSubscriber subscriber, BusEvent event) {
		this.events.add(Triple.of(Boolean.FALSE, subscriber, event));
	}
	
	public int length() {
		return this.events.size();
	}

	public boolean isDispatched(int index, Class<? extends EventBusSubscriber> subscriberType, Class<? extends BusEvent> eventType) {
		return this.events.get(index).getLeft() == Boolean.TRUE &&
				subscriberType.isAssignableFrom(this.events.get(index).getMiddle().getClass()) && 
				eventType.isAssignableFrom(this.events.get(index).getRight().getClass());
	}

	public boolean isReceival(int index, Class<? extends EventBusSubscriber> subscriberType, Class<? extends BusEvent> eventType) {
		return this.events.get(index).getLeft() == Boolean.FALSE &&
				subscriberType.isAssignableFrom(this.events.get(index).getMiddle().getClass()) && 
				eventType.isAssignableFrom(this.events.get(index).getRight().getClass());
	}
}
