package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Processor.Phase;
import com.mantledillusion.injection.hura.annotation.Inject;
import com.mantledillusion.injection.hura.annotation.Process;
import com.mantledillusion.injection.hura.annotation.Inject.SingletonMode;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe;

/**
 * Basic super type for a subscriber on the global event bus.
 * <p>
 * NOTE: Should be injected, since the {@link Injector} handles the instance's
 * life cycles.
 * <P>
 * The only implementation of the {@link EventBusSubscriber} on
 * framework side is the {@link Presenter}; that being said, an own
 * {@link EventBusSubscriber} implementation subscribes on the same
 * event bus as <B>all</B> presenters of the same {@link CottonUI} instance,
 * providing access to the bus' event traffic.
 * <P>
 * All methods of this {@link Presenter} implementation that are
 * annotated with @{@link Subscribe} will receive specifiable events of other
 * {@link EventBusSubscriber}s that were dispatched using the
 * {@link #dispatch(BusEvent)} {@link Method}.
 */
public class EventBusSubscriber {

	/**
	 * Super type for events to dispatch via the global
	 * {@link EventBusSubscriber} event bus.
	 */
	public static abstract class BusEvent {

		private Map<String, String> properties;

		/**
		 * Default {@link Constructor}.
		 */
		protected BusEvent() {
		}

		/**
		 * Convenience {@link Constructor} since most propertied events have 1 property
		 * set.
		 * 
		 * @param key
		 *            The key of the property; <b>not</b> allowed to be null.
		 * @param value
		 *            The value of the property; may be null.
		 */
		protected BusEvent(String key, String value) {
			addProperty(key, value);
		}

		/**
		 * Adds a property to the event. Subscribing {@link EventBusSubscriber}
		 * {@link Method}s may filter events for these properties using
		 * {@link Subscribe.EventProperty}s in the {@link Method}'s {@link Subscribe} annotation.
		 * 
		 * @param key
		 *            The key of the property; <b>not</b> allowed to be null.
		 * @param value
		 *            The value of the property; may be null.
		 */
		protected final void addProperty(String key, String value) {
			if (key == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The key of a property can never be null!");
			}
			if (this.properties == null) {
				this.properties = new HashMap<>();
			}
			this.properties.put(key, value);
		}

		/**
		 * Returns whether this event has ANY value set in this property.
		 * 
		 * @param key
		 *            The key to check for; might be null for convenience, although the
		 *            {@link Method} can only return false in this case since key-less
		 *            properties are not allowed.
		 * @return True when there is a property with the given key, false otherwise
		 */
		public final boolean hasProperty(String key) {
			return this.properties.containsKey(key);
		}

		/**
		 * Returns whether this event has a property with the given key and the value
		 * equals the given one.
		 *
		 * @param key
		 *            The key of the property; might be null for convenience, although
		 *            the {@link Method} can only return false in this case since
		 *            key-less properties are not allowed.
		 * @param value
		 *            The value of the property; may be null.
		 * @return True if there is a property with the given key and the value equals
		 *         the given one, false otherwise
		 */
		public final boolean equalProperty(String key, String value) {
			if (key == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The key of a property can never be null!");
			}
			return this.properties.containsKey(key)
					&& (value == null ? this.properties.get(key) == null : this.properties.get(key).equals(value));
		}

		/**
		 * Returns whether this event has properties for all of the given keys and their
		 * values equal the given ones.
		 * <P>
		 * Essentially, this is repeatedly calling
		 * {@link #equalProperty(String, String)} with all entries of the given map.
		 * 
		 * @param properties
		 *            The properties to check against; may be null.
		 * @return True if all properties in the given map equal the properties in this
		 *         event, false otherwise
		 */
		public final boolean equalProperties(Map<String, String> properties) {
			return properties == null || properties.isEmpty()
					|| properties.keySet().stream().allMatch(key -> (properties.get(key) == null
							&& this.properties.containsKey(key) && this.properties.get(key) == null)
							|| (properties.get(key) != null && properties.get(key).equals(this.properties.get(key))));
		}
	}

	@Inject(value = EventBus.PRESENTER_EVENT_BUS_ID, singletonMode = SingletonMode.GLOBAL)
	private EventBus bus;

	@Process
	private void initialize() {
		for (Method method : MethodUtils.getMethodsListWithAnnotation(getClass(), Subscribe.class, true, true)) {
			// PRESENTER EVENT METHODS
			if (method.isAnnotationPresent(Subscribe.class)) {
				if (!method.isAccessible()) {
					try {
						method.setAccessible(true);
					} catch (SecurityException e) {
						throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
								"Unable to gain access to the method '" + method.getName() + "' of the type "
										+ EventBusSubscriber.this.getClass().getSimpleName()
										+ " which is inaccessible.",
								e);
					}
				}

				Subscribe annotation = method.getAnnotation(Subscribe.class);


				Map<String, String> properties = null;
				if (annotation.value().length > 0) {
					properties = new HashMap<>();
					for (Subscribe.EventProperty property : annotation.value()) {
						if (properties.containsKey(property.key())) {
							throw new WebException(HttpErrorCodes.HTTP904_ILLEGAL_ANNOTATION_USE,
									"The event property key '" + property.key() + "' is used twice.");
						}
						properties.put(property.key(), property.value());
					}
				}

				if (method.getParameterCount() > 0) {
					Class<?> eventType = method.getParameterTypes()[0];
					
					@SuppressWarnings("unchecked")
					Class<? extends BusEvent> parameterEventType = (Class<? extends BusEvent>) eventType;
					
					this.bus.subscribe(parameterEventType, this, method, true, properties, annotation.isSelfObservant());
				}
				
				for (Class<? extends BusEvent> anonymousEventType: annotation.anonymousEvents()) {
					this.bus.subscribe(anonymousEventType, this, method, false, properties, annotation.isSelfObservant());
				}
			}
		}
	}

	@Process(Phase.DESTROY)
	private void releaseReferences() {
		this.bus.unsubscribe(this);
	}

	/**
	 * Dispatches the given {@link BusEvent} through the global event bus
	 * that links all {@link EventBusSubscriber}s via the {@link Subscribe}
	 * annotation on their {@link Method}s.
	 * 
	 * @param event
	 *            The event to dispatch; <b>not</b> allowed to be null.
	 * @return True if the {@link BusEvent} has been received by at least
	 *         one {@link EventBusSubscriber}, false otherwise
	 */
	protected final boolean dispatch(BusEvent event) {
		return this.bus.dispatch(event, this);
	}
}
