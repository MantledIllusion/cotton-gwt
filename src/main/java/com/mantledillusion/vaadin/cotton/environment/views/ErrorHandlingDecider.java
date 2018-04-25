package com.mantledillusion.vaadin.cotton.environment.views;

import com.mantledillusion.injection.hura.Blueprint;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.vaadin.server.ErrorHandler;

import javax.xml.ws.http.HTTPException;

/**
 * A decider for a specific error type.
 * <p>
 * The decider can either return an {@link ErrorView} {@link Blueprint.TypedBlueprint}
 * that is suitable to be used for a specific error instance or wrap the error
 * and re-throw it so it can get handled by a different decider.
 *
 * @param <ErrorType>
 *            The error type this decider can decide for.
 */
public interface ErrorHandlingDecider<ErrorType extends Throwable> {

    /**
     * Decides how to handle the given error.
     * <p>
     * If this decider is able to provide an {@link ErrorView} that can handle the
     * error, it returns a {@link Blueprint.TypedBlueprint} that can be used to instantiate
     * and inspect an instance of that view.
     * <P>
     * If it is not, it wraps the error in a {@link Throwable} and throws it to be
     * handled by a different decider. For example, a provider could catch
     * {@link HTTPException}s and wrap them into a {@link WebException}s with a
     * matching matching {@link WebException.HttpErrorCodes}. That {@link WebException} would
     * then be handled by a different {@link ErrorHandlingDecider} handling
     * {@link WebException}s.
     * <p>
     * Note that if the {@link Throwable} implementing type is thrown more than once
     * during handling an error it is assumed that the {@link ErrorHandlingDecider}s
     * have run into a loop, causing the whole handling to be interupted with an
     * {@link WebException.HttpErrorCodes#HTTP508_LOOP_DETECTED} {@link WebException} that is
     * directly given to the underlaying {@link ErrorHandler}.
     *
     * @param error
     *            The caught error; might <b>not</b> be null.
     * @return The {@link Blueprint.TypedBlueprint} to use for {@link ErrorView} retrieval
     *         that can handle the given error; never null
     * @throws Throwable
     *             The {@link Throwable} that should be handled by some other
     *             {@link ErrorHandlingDecider} instead of the given one
     */
    Blueprint.TypedBlueprint<? extends ErrorView<ErrorType>> decide(ErrorType error) throws Throwable;
}
