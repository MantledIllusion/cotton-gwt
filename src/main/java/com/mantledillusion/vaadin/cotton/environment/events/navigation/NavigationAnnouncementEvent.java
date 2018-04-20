package com.mantledillusion.vaadin.cotton.environment.events.navigation;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;

/**
 * {@link EventBusSubscriber.BusEvent} that is dispatched <b>before</b> a navigation is performed;
 * therefore giving subscribers the possibility to {@link #decline()} the
 * navigation.
 * <p>
 * Subscribers declining the navigation are expected to trigger user notifying
 * mechanisms, so the user becomes aware why he could not navigate.
 */
public final class NavigationAnnouncementEvent extends EventBusSubscriber.BusEvent {

    private final NavigationType navigationType;
    private final NavigationInitiator navigationInitiator;
    private boolean doAccept = true;

    public NavigationAnnouncementEvent(NavigationType navigationType, NavigationInitiator navigationInitiator) {
        this.navigationType = navigationType;
        this.navigationInitiator = navigationInitiator;
    }

    /**
     * Returns the type of navigation that is requested.
     *
     * @return The {@link NavigationType}; never null
     */
    public NavigationType getNavigationType() {
        return this.navigationType;
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
     * Marks the requested navigation to be declined.
     */
    public void decline() {
        this.doAccept = false;
    }

    /**
     * Returns whether the announced navigation is accepted by all retrievers of this event.
     *
     * @return True if no retriever has called {@link #decline()}, false otherwise
     */
    public boolean doAccept() {
        return doAccept;
    }
}
