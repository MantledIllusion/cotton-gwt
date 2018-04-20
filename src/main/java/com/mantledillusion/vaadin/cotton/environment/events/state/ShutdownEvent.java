package com.mantledillusion.vaadin.cotton.environment.events.state;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;
import com.vaadin.ui.UI;

/**
 * {@link EventBusSubscriber.BusEvent} that is dispatched on {@link UI}
 * shutdown.
 */
public final class ShutdownEvent extends EventBusSubscriber.BusEvent {

	public ShutdownEvent() {
	}
}
