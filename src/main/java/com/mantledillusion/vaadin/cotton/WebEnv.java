package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.mantledillusion.vaadin.cotton.environment.events.navigation.NavigationAnnouncementEvent;
import com.mantledillusion.vaadin.cotton.environment.events.user.UserChangeAnnouncementEvent;
import com.mantledillusion.vaadin.cotton.User.SessionLogContext;
import com.mantledillusion.vaadin.cotton.User.SessionLogEntry;
import com.mantledillusion.vaadin.cotton.User.SessionLogType;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Addressed;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

/**
 * Type that offers static methods in the web environment context of the current
 * {@link CottonUI} instance.
 */
public final class WebEnv {

	private WebEnv() {
	}

	// #########################################################################################################################################
	// ############################################################## NAVIGATION ###############################################################
	// #########################################################################################################################################

	/**
	 * Convenience {@link Method} for {@link #navigateTo(NavigationTarget)} with the
	 * given URL.
	 * 
	 * @param url
	 *            The URL to navigate to; <b>not</b> allowed to be null.
	 * @return True if the {@link NavigationAnnouncementEvent} is accepted and the
	 *         navigation was successful, false otherwise
	 */
	public static boolean navigateTo(String url) {
		return CottonUI.current().navigateTo(NavigationTarget.of(url));
	}

	/**
	 * Convenience {@link Method} for {@link #navigateTo(NavigationTarget)} with the
	 * given {@link View}.
	 * 
	 * @param viewClass
	 *            The {@link View} annotated with @{@link Addressed} to navigate to;
	 *            <b>not</b> allowed to be null.
	 * @return True if the {@link NavigationAnnouncementEvent} is accepted and the
	 *         navigation was successful, false otherwise
	 */
	public static boolean navigateTo(Class<? extends View> viewClass) {
		return CottonUI.current().navigateTo(NavigationTarget.of(viewClass));
	}

	/**
	 * Instructs the current {@link CottonUI} to navigate to the given target.
	 * 
	 * @param target
	 *            The target to navigate to; <b>not</b> allowed to be null.
	 * @return True if the {@link NavigationAnnouncementEvent} is accepted and the
	 *         navigation was successful, false otherwise
	 */
	public static boolean navigateTo(NavigationTarget target) {
		if (target == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Unable to navigate to a null target.");
		}
		return CottonUI.current().navigateTo(target);
	}

	/**
	 * Causes a reload at the current URL.
	 * 
	 * @return True if the {@link NavigationAnnouncementEvent} is accepted and the
	 *         refresh was successful, false otherwise
	 */
	public static boolean refresh() {
		return CottonUI.current().refresh();
	}

	// #########################################################################################################################################
	// ############################################################# LOCALIZATION ##############################################################
	// #########################################################################################################################################

	/**
	 * Returns whether the locale used by the current {@link CottonUI} instance is
	 * the default one.
	 * 
	 * @return True if the current locale is the default one, false otherwise
	 */
	public static boolean isCurrentDefaultLocale() {
		return CottonUI.current().isCurrentDefaultLocale();
	}

	/**
	 * Returns the {@link Locale} the current {@link CottonUI} instance is working
	 * with.
	 * 
	 * @return The current locale; will always be a language-only {@link Locale}
	 */
	public static Locale getCurrentLocale() {
		return CottonUI.current().getCurrentLocale();
	}

	/**
	 * Sets the {@link Locale} the current {@link CottonUI} instance has to work
	 * with.
	 * <P>
	 * Will cause a reload so the new language can be used on all components.
	 * <P>
	 * If the given {@link Locale} is not available, the {@link CottonUI} will
	 * switch to the default {@link Locale}.
	 * 
	 * @param locale
	 *            The {@link Locale} to set; might be null, which will cause a
	 *            switch to the default {@link Locale} if the current is not the
	 *            default {@link Locale}
	 */
	public static void setCurrentLocale(Locale locale) {
		CottonUI.current().setCurrentLocale(locale);
	}

	/**
	 * Localizes the given message identifier with the current {@link CottonUI}'s
	 * locale using the {@link ResourceBundle}s configured at the current
	 * {@link CottonUI} for that language.
	 * <P>
	 * Depending on the current language's {@link Locale}, the given message
	 * parameters may also be localized during insertion into the message.
	 * 
	 * @param msgId
	 *            The message id to localize; may be null or not even a message id.
	 * @param messageParameters
	 *            The parameters to inject into the localized message. Will only be
	 *            used if the message id could be localized.
	 * @return A localized and parameter filled message, or the given msgId if
	 *         localization was not possible
	 */
	public static String localize(String msgId, Object... messageParameters) {
		return CottonUI.current().localize(msgId, messageParameters);
	}

	// #########################################################################################################################################
	// ################################################################ LOG IN #################################################################
	// #########################################################################################################################################

	/**
	 * Will show the log in page and, after logging in, reload the current page.
	 */
	public static void showlogIn() {
		CottonUI.current().showlogIn();
	}

	/**
	 * Will log in the given {@link User} and reload the current page.
	 * 
	 * @param user
	 *            The {@link User} to log in; might <b>not</b> be null.
	 * @return True if the {@link UserChangeAnnouncementEvent} is accepted and the
	 *         log in was successful, false otherwise
	 */
	public static boolean logIn(User user) {
		if (user == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Unable to log in a null user.");
		}
		return CottonUI.current().logIn(user);
	}

	/**
	 * Will log out the current {@link User} if there is one; in that case, also
	 * reloads the current page.
	 * 
	 * @return True if the {@link UserChangeAnnouncementEvent} is accepted and the
	 *         log out was successful, false otherwise
	 */
	public static boolean logOut() {
		return CottonUI.current().logOut();
	}

	/**
	 * Returns whether there is currently a {@link User} logged in.
	 * 
	 * @return True if there is a {@link User} logged in, false otherwise
	 */
	public static boolean isLoggedIn() {
		return CottonUI.current().isLoggedIn();
	}

	/**
	 * Returns the currently logged in {@link User}, if there is one.
	 * 
	 * @return The {@link User} currently logged in, or null, if there is none
	 */
	public static User getLoggedInUser() {
		return CottonUI.current().getLoggedInUser();
	}

	// #########################################################################################################################################
	// ################################################################ RIGHTS #################################################################
	// #########################################################################################################################################

	/**
	 * Returns if the {@link User} that is currently logged in owns the given right.
	 * 
	 * @param userRightId
	 *            The user right that is checked whether the current user has it;
	 *            might <b>not</b> be null.
	 * @return True if there currently is a {@link User} and he owns the given
	 *         right, false otherwise
	 */
	public static boolean isAllowed(String userRightId) {
		return CottonUI.current().areAllowed(Collections.singleton(userRightId));
	}

	/**
	 * Returns if the {@link User} that is currently logged in owns the given right.
	 * 
	 * @param userRightIds
	 *            The user rights that are checked whether the current user has it;
	 *            might <b>not</b> be null.
	 * @return True if there currently is a {@link User} and he owns the given
	 *         right, false otherwise
	 */
	public static boolean areAllowed(Set<String> userRightIds) {
		return CottonUI.current().areAllowed(userRightIds);
	}

	// #########################################################################################################################################
	// ############################################################# SESSION LOG ###############################################################
	// #########################################################################################################################################

	/**
	 * Appends the given {@link SessionLogEntry}s to this session's log.
	 * 
	 * @param entries
	 *            The entries to add; might be null or contain null, these are
	 *            ignored
	 */
	public static void appendToLog(SessionLogEntry... entries) {
		CottonUI.current().appendToLog(entries);
	}

	/**
	 * Returns whether there currently is a {@link SessionLogEntry} in the session
	 * log who is of the given {@link SessionLogContext}.
	 * 
	 * @param context
	 *            The context to search for; might be null, although the result will
	 *            always be false
	 * @return True if there is an entry of the given context, false otherwise
	 */
	public static boolean hasLogEntryOfContext(SessionLogContext context) {
		return CottonUI.current().hasLogEntryOfContext(context);
	}

	/**
	 * Returns whether there currently is a {@link SessionLogEntry} in the session
	 * log who is of the given {@link SessionLogType}.
	 * 
	 * @param type
	 *            The type to search for; might be null, although the result will
	 *            always be false
	 * @return True if there is an entry of the given type, false otherwise
	 */
	public static boolean hasLogEntryOfType(SessionLogType type) {
		return CottonUI.current().hasLogEntryOfType(type);
	}

	/**
	 * Returns whether there currently is a {@link SessionLogEntry} in the session
	 * log who is of the given {@link SessionLogContext} and {@link SessionLogType}.
	 * 
	 * @param context
	 *            The context to search for; might be null, although the result will
	 *            always be false
	 * @param type
	 *            The type to search for; might be null, although the result will
	 *            always be false
	 * @return True if there is an entry of the given context and type, false
	 *         otherwise
	 */
	public static boolean hasLogEntryOfContextAndType(SessionLogContext context, SessionLogType type) {
		return CottonUI.current().hasLogEntryOfContextAndType(context, type);
	}

	/**
	 * Returns an unmodifyable view of the current session log.
	 * 
	 * @return The current log; never null
	 */
	public static List<SessionLogEntry> getLog() {
		return CottonUI.current().getLog();
	}
}
