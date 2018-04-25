package com.mantledillusion.vaadin.cotton.environment.views;

import com.mantledillusion.vaadin.cotton.CottonServlet;
import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

/**
 * Specialized {@link View} sub type that can be supplied to the configuration
 * of a {@link CottonServlet} during its configuration phase.
 * <p>
 * Whenever there is a need for a paged log in, the implementation of this
 * {@link LoginView} configured there will be used automatically.
 * <p>
 * When a {@link LoginView} instance is displayed, a call to
 * {@link WebEnv#logIn(com.mantledillusion.vaadin.cotton.User)} will cause the
 * {@link LoginView} to be closed and a redirect to the last visited view will
 * be executed.
 */
public abstract class LoginView extends View {

	private static final long serialVersionUID = 1L;
}
