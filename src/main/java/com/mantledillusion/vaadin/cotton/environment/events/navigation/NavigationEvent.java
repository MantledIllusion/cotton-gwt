package com.mantledillusion.vaadin.cotton.environment.events.navigation;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;

/**
 * {@link EventBusSubscriber.BusEvent} that is dispatched after a navigation has been performed.
 */
public final class NavigationEvent extends EventBusSubscriber.BusEvent {

    private final NavigationType navigationType;
    private final NavigationInitiator navigationInitiator;

    public NavigationEvent(NavigationType navigationType, NavigationInitiator navigationInitiator) {
        this.navigationType = navigationType;
        this.navigationInitiator = navigationInitiator;
    }

    /**
     * Returns the initiator of the requested navigation.
     *
     * @return The {@link NavigationInitiator}; never null
     */
    public NavigationInitiator getNavigationInitiator() {
        return navigationInitiator;
    }

    /**
     * Returns the type of navigation that has been performed.
     *
     * @return The {@link NavigationType}; never null
     */
    public NavigationType getNavigationType() {
        return this.navigationType;
    }
}
