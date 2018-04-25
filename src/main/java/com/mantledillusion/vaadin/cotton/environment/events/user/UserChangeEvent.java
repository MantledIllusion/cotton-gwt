package com.mantledillusion.vaadin.cotton.environment.events.user;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;

/**
 * {@link EventBusSubscriber.BusEvent} that is dispatched after a user change has been performed.
 */
public final class UserChangeEvent extends EventBusSubscriber.BusEvent {

    private final UserChangeType changeType;

    public UserChangeEvent(UserChangeType changeType) {
        this.changeType = changeType;
    }

    /**
     * Returns the type of user change that has been performed.
     *
     * @return The {@link UserChangeType}; never null
     */
    public UserChangeType getChangeType() {
        return changeType;
    }
}
