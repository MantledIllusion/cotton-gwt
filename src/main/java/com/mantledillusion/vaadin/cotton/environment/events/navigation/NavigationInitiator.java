package com.mantledillusion.vaadin.cotton.environment.events.navigation;

import com.mantledillusion.vaadin.cotton.WebEnv;

/**
 * The initiators of navigation changes that are possible.
 */
public enum NavigationInitiator {

	/**
	 * The browser; manual navigation through back/forward, refresh or URl type in.
	 */
	BROWSER,

	/**
	 * The server; programmatic navigation through {@link WebEnv} or
	 * similar.
	 */
	SERVER;
}
