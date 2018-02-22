package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Restricted;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

/**
 * Interface for types that represent a single {@link User} of an
 * {@link CottonUI}.
 */
public interface User {

	/**
	 * The originator of a {@link SessionLogEntry}.
	 */
	public static enum SessionLogAviator {
		COTTON, APPLICATION;
	}

	/**
	 * The context in which a {@link SessionLogEntry} was created.
	 */
	public static enum SessionLogContext {
		SESSION, NAVIGATION, USER, ACTION;
	}

	/**
	 * The type of {@link SessionLogEntry}.
	 */
	public static enum SessionLogType {
		INFO, WARNING, ERROR;
	}

	/**
	 * Represents a single entry in the session log of a {@link CottonUI}
	 * implementation instance.
	 */
	public static final class SessionLogEntry {

		private final SessionLogAviator aviator;
		private final SessionLogContext context;
		private final SessionLogType type;
		private final String message;
		private final LocalDateTime timestamp;

		private SessionLogEntry(SessionLogAviator aviator, SessionLogContext context, SessionLogType type,
				String message) {
			this.aviator = aviator;
			this.context = context;
			this.type = type;
			this.message = message;
			this.timestamp = LocalDateTime.now();
		}

		/**
		 * Returns the creator of this entry.
		 * 
		 * @return The {@link SessionLogAviator} that created this entry; never null
		 */
		public SessionLogAviator getAviator() {
			return aviator;
		}

		/**
		 * Returns the context this entry was created in.
		 * 
		 * @return The {@link SessionLogContext} this entry was created in; never null
		 */
		public SessionLogContext getContext() {
			return context;
		}

		/**
		 * Returns the type of this entry.
		 * 
		 * @return The {@link SessionLogType} of this entry; never null
		 */
		public SessionLogType getType() {
			return type;
		}

		/**
		 * Returns the message of this entry.
		 * 
		 * @return The message of this {@link SessionLogEntry}; might be null if the
		 *         entry was created that way
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * Returns the timestamp at which this entry was created.
		 * 
		 * @return The creation timestamp; never null
		 */
		public LocalDateTime getTimestamp() {
			return timestamp;
		}

		/**
		 * Creates a {@link SessionLogEntry} for the application.
		 * <p>
		 * The creator will be {@link SessionLogAviator#APPLICATION} and the context
		 * {@link SessionLogContext#ACTION} automatically. Also the timestamp will be of
		 * the moment this {@link Method} is called.
		 * 
		 * @param type
		 *            The {@link SessionLogType} of the new entry; might <b>not</b> be
		 *            null.
		 * @param message
		 *            The message of the new entry; might be null.
		 * @return A new {@link SessionLogEntry} instance; never null
		 */
		public static SessionLogEntry of(SessionLogType type, String message) {
			return of(SessionLogAviator.APPLICATION, SessionLogContext.ACTION, type, message);
		}

		static SessionLogEntry of(SessionLogContext context, SessionLogType type, String message) {
			return of(SessionLogAviator.COTTON, context, type, message);
		}

		private static SessionLogEntry of(SessionLogAviator aviator, SessionLogContext context, SessionLogType type,
				String message) {
			if (context == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot create a log entry for a null context.");
			} else if (type == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot create a log entry for a null type.");
			}
			return new SessionLogEntry(aviator, context, type, message);
		}
	}

	/**
	 * Has to return whether this {@link User} instance owns the rights of the given
	 * rightIds.
	 * <P>
	 * This method will be called whenever the {@link User} tries to navigate to a
	 * URL whose {@link View} is annotated with @{@link Restricted} with 1 or more
	 * given rightIds or the {@link WebEnv} is asked for the current {@link User}'s
	 * rights.
	 * 
	 * @param rightIds
	 *            The IDs of the rights this {@link User} is asked to have; never
	 *            null, might <b>not</b> be empty.
	 * @return True if the {@link User} owns <b>all</b> of the rights behind the
	 *         given IDs, false otherwise.
	 */
	boolean hasRights(Set<String> rightIds);
}
