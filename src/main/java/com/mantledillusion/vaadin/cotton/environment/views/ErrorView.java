package com.mantledillusion.vaadin.cotton.environment.views;

import com.mantledillusion.vaadin.cotton.CottonServlet;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

/**
 * Specialized {@link View} sub type that can be supplied to the configuration
 * of a {@link CottonServlet} during its configuration phase.
 * <p>
 * An {@link ErrorView} is meant to handle a specific type of {@link Throwable}
 *
 * @param <ErrorType>
 *            The {@link Throwable} sub type whose occurrences should be handled
 *            by the {@link ErrorView} implementation.
 */
public abstract class ErrorView<ErrorType extends Throwable> extends View {

	private static final long serialVersionUID = 1L;

	/**
	 * Will be called after the view has been instantiated for handling exactly one
	 * error of the {@link Throwable} sub type the {@link ErrorView} was registered
	 * for..
	 *
	 * @param t
	 *            The error that occurred.
	 */
	public abstract void handleError(ErrorType t);
}
