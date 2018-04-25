package com.mantledillusion.vaadin.cotton.environment.events.user;

import com.mantledillusion.vaadin.cotton.User;

/**
 * The types of {@link User} changes that are possible.
 */
public enum UserChangeType {

    /**
     * Change type of no user before -&gt; new user after.
     */
    LOGIN,

    /**
     * Change type of old user before -&gt; no user after.
     */
    LOGOUT;
}
