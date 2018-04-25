package com.mantledillusion.vaadin.cotton.environment.events.user;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;

/**
 * {@link EventBusSubscriber.BusEvent} that is dispatched <b>before</b> a user change is performed;
 * therefore giving subscribers the possibility to {@link #decline()} the user
 * change.
 * <p>
 * Subscribers declining the user change are expected to trigger user notifying
 * mechanisms, so the user becomes aware why the user change could not be
 * performed.
 */
public final class UserChangeAnnouncementEvent extends EventBusSubscriber.BusEvent {

    private final UserChangeType changeType;
    private boolean doAccept = true;
    private boolean doRefresh = false;

    public UserChangeAnnouncementEvent(UserChangeType changeType) {
        this.changeType = changeType;
    }

    /**
     * Returns the type of user change that is requested.
     *
     * @return The {@link UserChangeType}; never null
     */
    public UserChangeType getChangeType() {
        return changeType;
    }

    /**
     * Marks the requested user change to be declined.
     */
    public void decline() {
        this.doAccept = false;
    }

    /**
     * Marks the requested user change to cause a refresh when performed.
     */
    public void refreshAfterChange() {
        this.doRefresh = true;
    }

    /**
     * Returns whether the announced user change is accepted by all retrievers of this event.
     *
     * @return True if no retriever has called {@link #decline()}, false otherwise
     */
    public boolean doAccept() {
        return doAccept;
    }

    /**
     * Returns whether the announced user change should cause an refresh when it is executed.
     *
     * @return True if at least one retriever  of this event has called {@link #refreshAfterChange()}, false otherwise
     */
    public boolean doRefresh() {
        return doRefresh;
    }
}
