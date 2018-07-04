package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber.BusEvent;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

final class EventBus {
	
	static final String PRESENTER_EVENT_BUS_ID = "_presenterEventBus";

	private final class Subscriber {

		final EventBusSubscriber subscriber;
		final Map<Method, Subscribable> methods = new HashMap<>();

		private Subscriber(EventBusSubscriber presenter) {
			this.subscriber = presenter;
		}
	}

	private final class Subscribable {

		final boolean methodRequiresEvent;
		final boolean isSelfObservant;
		final Map<String, String> properties;

		private Subscribable(boolean methodRequiresEvent, boolean isSelfObservant, Map<String, String> properties) {
			this.methodRequiresEvent = methodRequiresEvent;
			this.isSelfObservant = isSelfObservant;
			this.properties = properties;
		}
	}

	private final Map<Class<? extends BusEvent>, IdentityHashMap<EventBusSubscriber, Subscriber>> subscribers = new HashMap<>();
	
	EventBus() {
	}

	synchronized void subscribe(Class<? extends BusEvent> eventType, EventBusSubscriber subscriber,
			Method m, boolean methodRequiresEvent, Map<String, String> properties, boolean isSelfObservant) {
		if (!this.subscribers.containsKey(eventType)) {
			this.subscribers.put(eventType, new IdentityHashMap<>());
		}
		if (!this.subscribers.get(eventType).containsKey(subscriber)) {
			this.subscribers.get(eventType).put(subscriber, new Subscriber(subscriber));
		}
		this.subscribers.get(eventType).get(subscriber).methods.put(m, new Subscribable(methodRequiresEvent, isSelfObservant, properties));
	}

	synchronized void unsubscribe(EventBusSubscriber subscriber) {
		Iterator<Entry<Class<? extends BusEvent>, IdentityHashMap<EventBusSubscriber, Subscriber>>> iter = subscribers.entrySet()
				.iterator();

		while (iter.hasNext()) {
			Entry<Class<? extends BusEvent>, IdentityHashMap<EventBusSubscriber, Subscriber>> entry = iter.next();

			entry.getValue().remove(subscriber);

			if (entry.getValue().isEmpty()) {
				iter.remove();
			}
		}
	}

	synchronized boolean dispatch(BusEvent event, EventBusSubscriber dispatcher) {
		if (event == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Unable to throw a null event.");
		}

		boolean subscriberFound = false;

		Class<?> eventType = event.getClass();
		while (eventType != BusEvent.class) {
			if (this.subscribers.containsKey(eventType)) {
				subscriberFound = true;
				for (Subscriber subscriber : this.subscribers.get(eventType).values()) {
					boolean isDispatcher = subscriber.subscriber == dispatcher;

					for (Method m : subscriber.methods.keySet()) {
						Subscribable subscribable = subscriber.methods.get(m);
						if (!isDispatcher || subscribable.isSelfObservant) {
							if (!event.equalProperties(subscribable.properties)) {
								continue;
							}

							try {
								if (subscribable.methodRequiresEvent) {
									m.invoke(subscriber.subscriber, event);
								} else if (m.getParameterCount() == 1) {
									m.invoke(subscriber.subscriber, new Object[] {null});
								} else {
									m.invoke(subscriber.subscriber);
								}
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								throw new WebException(HttpErrorCodes.HTTP500_INTERNAL_SERVER_ERROR,
										"Unable to dispatch event of type " + event.getClass().getSimpleName()
												+ " to a subscriber of the type "
												+ subscriber.subscriber.getClass().getSimpleName(),
										e);
							}
						}
					}
				}
			}
			eventType = eventType.getSuperclass();
		}

		return subscriberFound;
	}
}