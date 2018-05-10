package com.mantledillusion.vaadin.cotton;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;

import com.mantledillusion.vaadin.cotton.environment.events.navigation.NavigationAnnouncementEvent;
import com.mantledillusion.vaadin.cotton.environment.events.navigation.NavigationEvent;
import com.mantledillusion.vaadin.cotton.environment.events.navigation.NavigationInitiator;
import com.mantledillusion.vaadin.cotton.environment.events.navigation.NavigationType;
import com.mantledillusion.vaadin.cotton.environment.events.state.ShutdownEvent;
import com.mantledillusion.vaadin.cotton.environment.events.user.UserChangeAnnouncementEvent;
import com.mantledillusion.vaadin.cotton.environment.events.user.UserChangeEvent;
import com.mantledillusion.vaadin.cotton.environment.events.user.UserChangeType;
import com.mantledillusion.vaadin.cotton.environment.views.ErrorView;
import com.mantledillusion.vaadin.cotton.environment.views.LoginView;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Injector.RootInjector;
import com.mantledillusion.injection.hura.Predefinable.Singleton;
import com.mantledillusion.vaadin.cotton.User.SessionLogContext;
import com.mantledillusion.vaadin.cotton.User.SessionLogEntry;
import com.mantledillusion.vaadin.cotton.User.SessionLogType;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.server.Page.PopStateEvent;
import com.vaadin.server.Page.PopStateListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.UI;

final class CottonUI extends com.vaadin.ui.UI {

	private static final long serialVersionUID = 1L;
	static final Logger LOGGER = LoggerFactory.getLogger(CottonUI.class);

	private static final String REGEX_MESSAGE_ID_NAME_SEGMENT = "[^\\.\\s]+";
	static final String REGEX_TYPICAL_MESSAGE_ID = REGEX_MESSAGE_ID_NAME_SEGMENT + "(\\."
			+ REGEX_MESSAGE_ID_NAME_SEGMENT + ")+";
	private static final DateTimeFormatter COOKIE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy kk:mm:ss z");
	private static final String QUERY_PARAM_KEY_LANGUAGE = "lang";

	// INJECTION
	private final EventBus eventBus = new EventBus();
	private final RootInjector injector;

	// NAVIGATION
	private final UrlResourceRegistry urlRegistry;

	// LOCALIZATION
	private final String defaultLang;
	private final Map<String, LocalizationResource> resourceBundleRegistry;

	// LOGIN
	private final TypedBlueprint<? extends LoginView> loginViewBlueprint;

	// ERROR HANDLING
	private final InternalErrorHandler internalErrorHandler;
	private final List<SessionLogEntry> log = new ArrayList<>();

	// CURRENT
	private String currentUrl;
	private Map<String, String[]> currentParams;
	private final Map<String, CookieInstance> currentCookies = new HashMap<>();
	private View currentView;
	private User user;

	CottonUI(CottonServlet.TemporalCottonServletConfiguration config) {
		Singleton eventBus = Singleton.of(EventBus.PRESENTER_EVENT_BUS_ID, this.eventBus);
		this.injector = Injector.of(ListUtils.union(config.getPredefinables(), Arrays.asList(eventBus)));

		this.urlRegistry = config.getUrlRegistry();

		this.defaultLang = config.getDefaultLang();
		this.resourceBundleRegistry = config.getResourceBundleRegistry();

		this.loginViewBlueprint = config.getLoginViewBlueprint();

		this.internalErrorHandler = config.getInternalErrorHandler();
		this.isInternalErrorHandler = true;
		setErrorHandler(this.internalErrorHandler);
		this.isInternalErrorHandler = false;
	}
	
	UrlResourceRegistry getUrlRegistry() {
		return this.urlRegistry;
	}

	@Override
	protected final void init(VaadinRequest request) {
		try {
			appendToLog(SessionLogEntry.of(SessionLogContext.SESSION, SessionLogType.INFO,
					"Initializing session '" + request.getWrappedSession().getId() + "'"));

			if (request.getCookies() != null) {
				for (Cookie cookie : request.getCookies()) {
					this.currentCookies.put(cookie.getName(),
							new CookieInstance(cookie.getValue(), cookie.getMaxAge()));
				}
			}

			Page.getCurrent().addPopStateListener(new PopStateListener() {

				private static final long serialVersionUID = 1L;

				@Override
				public void uriChanged(PopStateEvent event) {
					handlePathChanged(QueryParam.fromParamAppender(event.getPage().getLocation().getQuery()));
				}
			});

			if (this.urlRegistry == null) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot initialize a UI using a null URL registry.");
			}

			handleRequest(request);
			
			handleRequest(request);
		} catch (Exception e) {
			this.internalErrorHandler.error(new com.vaadin.server.ErrorEvent(e));
			close();
		}
	}

	@Override
	protected final void refresh(VaadinRequest request) {
		handleRequest(request);
	}

	private void handleRequest(VaadinRequest request) {
		try {
			handlePathChanged(QueryParam.clean(request.getParameterMap()));
		} catch (Exception e) {
			this.internalErrorHandler.error(new com.vaadin.server.ErrorEvent(e));
		}
	}
	
	@Override
	public final void detach() {
		if (getSession() != null && getSession().getSession() != null) {
			appendToLog(SessionLogEntry.of(SessionLogContext.SESSION, SessionLogType.INFO,
					"Closing session '" + getSession().getSession().getId() + "'"));
		} else {
			appendToLog(SessionLogEntry.of(SessionLogContext.SESSION, SessionLogType.INFO,
					"Closing expired session"));
		}

		try {
			this.eventBus.dispatch(new ShutdownEvent(), null);
		} catch (Throwable t) {
			throw new WebException(HttpErrorCodes.HTTP500_INTERNAL_SERVER_ERROR, "The UI instance '" + this
					+ "' could not be shutdown correctly; an error occurred during notifying event bus subscribers of the shutdown.",
					t);
		} finally {
			super.detach();
			this.injector.destroyInjector();
		}
	}

	static CottonUI current() {
		UI vaadinUI = getCurrent();
		if (vaadinUI == null) {
			throw new WebException(HttpErrorCodes.HTTP900_UI_INACTIVITY_ERROR,
					"There is currently no " + CottonUI.class.getSimpleName() + " instance active "
							+ " as there has to be to retrieve the current " + CottonUI.class.getSimpleName()
							+ " instance statically.");
		} else if (!(vaadinUI instanceof CottonUI)) {
			throw new WebException(HttpErrorCodes.HTTP900_UI_INACTIVITY_ERROR,
					"The currently active UI type is " + vaadinUI.getClass().getName() + ", not a sub type of "
							+ CottonUI.class.getName() + " as it has to be to retrieve the current "
							+ CottonUI.class.getSimpleName() + " instance statically.");
		}
		return (CottonUI) vaadinUI;
	}

	// #########################################################################################################################################
	// ############################################################## NAVIGATION ###############################################################
	// #########################################################################################################################################

	private void handlePathChanged(Map<String, String[]> params) {
		String path = Page.getCurrent().getLocation().getPath();
		boolean changedLang = false;
		if (params.containsKey(QUERY_PARAM_KEY_LANGUAGE)) {
			String available = null;
			List<String> ignored = new ArrayList<>();
			for (String lang : params.get(QUERY_PARAM_KEY_LANGUAGE)) {
				if (this.resourceBundleRegistry.containsKey(lang)) {
					available = lang;
					break;
				} else {
					ignored.add(lang);
				}
			}

			if (!ignored.isEmpty()) {
				CottonUI.LOGGER.debug("Ignored missing languages ['" + StringUtils.join(ignored, "', '") + "']; "
						+ (available == null ? "using default '" + this.defaultLang + "' instead."
								: "using '" + available + "' instead."));
			}

			if (available == null) {
				params.remove(QUERY_PARAM_KEY_LANGUAGE);
				changedLang = true;
			} else if (params.get(QUERY_PARAM_KEY_LANGUAGE).length > 1) {
				params.put(QUERY_PARAM_KEY_LANGUAGE, new String[] { available });
				changedLang = true;
			}
		}

		String clean = clean(path);

		// Adds missing trailing /
		if (path.equals('/' + clean)) {
			updateUrlTo('/' + clean, params, false);
		}

		if (!changedLang) {
			navigate(clean, params, false, false, NavigationInitiator.BROWSER);
		} else {
			Page.getCurrent().setLocation(buildFullUrl(clean, params));
		}
	}

	private static String clean(String path) {
		ArrayList<String> splitted = new ArrayList<>();
		for (String part : path.split("/")) {
			if (StringUtils.isNotBlank(part)) {
				splitted.add(part);
			}
		}
		return StringUtils.join(splitted, '/');
	}

	private static String buildFullUrl(String basePath, Map<String, String[]> params) {
		URI uri = Page.getCurrent().getLocation();
		return uri.getScheme() + "://" + uri.getAuthority() + '/' + basePath
				+ (basePath.isEmpty() ? StringUtils.EMPTY : '/') + QueryParam.toParamAppender(params);
	}

	private void updateUrl(boolean createBrowserNavEntry) {
		updateUrlTo(this.currentUrl, this.currentParams, createBrowserNavEntry);
	}

	private void updateUrlTo(String basePath, Map<String, String[]> params, boolean createBrowserNavEntry) {
		String queryParams = QueryParam.toParamAppender(params);
		String url = '/' + basePath + (basePath.isEmpty() ? StringUtils.EMPTY : '/') + queryParams;
		if (createBrowserNavEntry) {
			Page.getCurrent().pushState(url);
		} else {
			Page.getCurrent().replaceState(url);
		}
	}

	private void notifyNavigationAwares(NavigationType navigationChangeType, NavigationInitiator navigationInitiator) {
		this.eventBus.dispatch(new NavigationEvent(navigationChangeType, navigationInitiator), null);
	}

	private boolean navigate(String urlPath, Map<String, String[]> params, boolean keepLanguageParam,
			boolean createBrowserNavEntry, NavigationInitiator navigationInitiator) {
		NavigationType navigationType;
		if (!urlPath.equals(this.currentUrl)) {
			navigationType = NavigationType.SEGMENT_CHANGE;
		} else if (containsChangedParamsButUnchangedLanguage(params)) {
			navigationType = NavigationType.QUERY_PARAM_CHANGE;
		} else {
			navigationType = NavigationType.REFRESH;
		}

		if (this.urlRegistry.hasRedirectAt(urlPath)) {
			String redirectedPath = this.urlRegistry.getRedirectAt(urlPath);
			LOGGER.debug("URL segment path '" + urlPath + "' has been redirected to '" + redirectedPath + "'");
			urlPath = redirectedPath;
		}

		if (isNavigationAllowed(navigationType, navigationInitiator)) {
			if (keepLanguageParam && !params.containsKey(QUERY_PARAM_KEY_LANGUAGE)
					&& this.currentParams.containsKey(QUERY_PARAM_KEY_LANGUAGE)) {
				params.put(QUERY_PARAM_KEY_LANGUAGE, this.currentParams.get(QUERY_PARAM_KEY_LANGUAGE));
			}

			this.currentUrl = urlPath;
			this.currentParams = new HashMap<>(params);

			if (navigationType != NavigationType.REFRESH) {
				appendToLog(SessionLogEntry.of(SessionLogContext.NAVIGATION, SessionLogType.INFO,
						"Navigated to '" + buildFullUrl(this.currentUrl, this.currentParams) + "'"));
				updateUrl(createBrowserNavEntry);
			}

			if (navigationType != NavigationType.QUERY_PARAM_CHANGE || currentView instanceof ErrorView) {
				if (!this.urlRegistry.hasViewAt(urlPath)) {
					if (this.urlRegistry.hasGoneAt(urlPath)) {
						throw new WebException(HttpErrorCodes.HTTP410_GONE,
								"The requested resource at '" + urlPath + "' is not existing anymore.");
					} else {
						throw new WebException(HttpErrorCodes.HTTP404_NOT_FOUND,
								"There is no resource registered for the path '" + urlPath + "'");
					}
				}

				try {
					doDisplay(this.urlRegistry.getViewAt(urlPath));
				} catch (Throwable t) {
					Throwable cause = ObjectUtils.defaultIfNull(ExceptionUtils.getRootCause(t), t);
					if (cause instanceof WebException
							&& ((WebException) cause).getErrorCode() == HttpErrorCodes.HTTP403_FORBIDDEN
							&& this.user == null) {
						if (this.loginViewBlueprint != null) {
							showlogIn();
						} else {
							throw new WebException(HttpErrorCodes.HTTP403_FORBIDDEN, "The requested registration at '"
									+ urlPath
									+ "' requires authorization, but there is no user currently logged in and no login page to auto transfer to; "
									+ cause, cause);
						}
					} else {
						throw t;
					}
				}
			}

			notifyNavigationAwares(navigationType, navigationInitiator);
			return true;
		} else {
			appendToLog(SessionLogEntry.of(SessionLogContext.NAVIGATION, SessionLogType.WARNING,
					"Navigation to '" + buildFullUrl(urlPath, params) + "' denied."));
			updateUrl(createBrowserNavEntry);
			return false;
		}
	}
	
	boolean containsChangedParamsButUnchangedLanguage(Map<String, String[]> params) {
		if (!Arrays.equals(params.get(QUERY_PARAM_KEY_LANGUAGE),
				this.currentParams.get(QUERY_PARAM_KEY_LANGUAGE))) {
			return false;
		} else if (!this.currentParams.keySet().equals(params.keySet())) {
			return true;
		} else {
			for (String key: params.keySet()) {
				if (!Arrays.equals(params.get(key), this.currentParams.get(key))) {
					return true;
				}
			}
			return false;
		}
	}

	<ViewType extends View> ViewType doDisplay(TypedBlueprint<ViewType> viewType) {
		if (this.currentView != null) {
			this.injector.destroy(this.currentView);
			this.currentView = null;
		}
		ViewType view = this.injector.instantiate(viewType);
		setContent(view);
		this.currentView = view;
		return view;
	}

	private boolean isNavigationAllowed(NavigationType navigationChangeType, NavigationInitiator navigationInitiator) {
		NavigationAnnouncementEvent event = new NavigationAnnouncementEvent(navigationChangeType, navigationInitiator);
		this.eventBus.dispatch(event, null);
		return event.doAccept();
	}

	// ########## Internally Usable ##########

	final boolean hasQueryParam(String paramKey) {
		return this.currentParams.containsKey(paramKey);
	}

	final String[] getQueryParam(String paramKey) {
		return hasQueryParam(paramKey) ? this.currentParams.get(paramKey) : new String[0];
	}

	final void setQueryParam(String key, String... values) {
		this.currentParams.put(key, values);
		updateUrl(false);
		appendToLog(SessionLogEntry.of(SessionLogContext.NAVIGATION, SessionLogType.INFO,
				"Query param '" + key + "' set to [" + StringUtils.join(values, '/') + "] set."));
		notifyNavigationAwares(NavigationType.QUERY_PARAM_CHANGE, NavigationInitiator.SERVER);
	}

	final void removeQueryParam(String key) {
		if (this.currentParams.containsKey(key)) {
			String[] values = currentParams.get(key);
			this.currentParams.remove(key);
			updateUrl(false);
			appendToLog(SessionLogEntry.of(SessionLogContext.NAVIGATION, SessionLogType.INFO,
					"Query param '" + key + "' with values [" + StringUtils.join(values, '/') + "] removed."));
			notifyNavigationAwares(NavigationType.QUERY_PARAM_CHANGE, NavigationInitiator.SERVER);
		}
	}

	final boolean navigateTo(NavigationTarget target) {
		return navigate(target.getUrl(), target.getParams(), true, true, NavigationInitiator.SERVER);
	}

	final boolean refresh() {
		return navigate(this.currentUrl, this.currentParams, true, false, NavigationInitiator.SERVER);
	}

	// #########################################################################################################################################
	// ################################################################ COOKIE #################################################################
	// #########################################################################################################################################

	private final class CookieInstance {

		private final String value;
		private final ZonedDateTime expiringDate;

		private CookieInstance(String value, ZonedDateTime expiringDate) {
			this.value = value;
			this.expiringDate = expiringDate;
		}

		private CookieInstance(String value, int maxAge) {
			this.value = value;
			this.expiringDate = maxAge < 0 ? null : ZonedDateTime.now().plus(maxAge, ChronoUnit.SECONDS);
		}

		private boolean isExpired() {
			return this.expiringDate != null && this.expiringDate.isBefore(ZonedDateTime.now());
		}
	}

	final boolean hasCookie(String name) {
		return this.currentCookies.containsKey(name) && !this.currentCookies.get(name).isExpired();
	}

	final String getCookie(String name) {
		if (hasCookie(name)) {
			return this.currentCookies.get(name).value;
		} else {
			return null;
		}
	}

	final void setCookie(String name, String value, ZonedDateTime expiringDate) {
		JavaScript.getCurrent().execute("document.cookie = \"" + stringifyedCookie(name, value, expiringDate) + "\"");
		this.currentCookies.put(name, new CookieInstance(value, expiringDate));

		if (expiringDate == null || expiringDate.isAfter(ZonedDateTime.now())) {
			appendToLog(SessionLogEntry.of(SessionLogContext.ACTION, SessionLogType.INFO,
					"Cookie '" + name + "' set to '" + value + "'"));
		} else {
			appendToLog(
					SessionLogEntry.of(SessionLogContext.ACTION, SessionLogType.INFO, "Cookie '" + name + "' expired"));
		}
	}

	private String stringifyedCookie(String name, String value, ZonedDateTime expiringDate) {
		return name + '=' + value + ';'
				+ (expiringDate != null ? COOKIE_DATE_FORMAT.format(expiringDate) + ';' : StringUtils.EMPTY)
				+ "path=/;";
	}

	// #########################################################################################################################################
	// ############################################################# LOCALIZATION ##############################################################
	// #########################################################################################################################################

	// ########## Internally Usable ##########

	final boolean isCurrentDefaultLocale() {
		return !this.currentParams.containsKey(QUERY_PARAM_KEY_LANGUAGE)
				|| this.defaultLang.equals(this.currentParams.get(QUERY_PARAM_KEY_LANGUAGE)[0]);
	}

	final Locale getCurrentLocale() {
		return new Locale(this.currentParams.containsKey(QUERY_PARAM_KEY_LANGUAGE)
				? this.currentParams.get(QUERY_PARAM_KEY_LANGUAGE)[0]
				: this.defaultLang);
	}

	final void setCurrentLocale(Locale locale) {
		if (locale != null) {
			if (StringUtils.isBlank(locale.getISO3Language())) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The given locale's ISO3 language was blank; cannot switch the UI to a blank language.");
			}
			if (!locale.getISO3Language().equals(getCurrentLocale().getISO3Language())) {
				if (locale.getISO3Language().equals(this.defaultLang)) {
					removeQueryParam(QUERY_PARAM_KEY_LANGUAGE);
					refresh();
				} else if (this.resourceBundleRegistry.containsKey(locale.getISO3Language())) {
					setQueryParam(QUERY_PARAM_KEY_LANGUAGE, locale.getISO3Language());
					refresh();
				} else {
					CottonUI.LOGGER.debug(
							"Ignored missing language '" + locale.getISO3Language() + "'; using default instead.");
					if (!isCurrentDefaultLocale()) {
						removeQueryParam(QUERY_PARAM_KEY_LANGUAGE);
						refresh();
					}
				}
			}
		} else if (!isCurrentDefaultLocale()) {
			setQueryParam(QUERY_PARAM_KEY_LANGUAGE, this.defaultLang);
			refresh();
		}
	}

	final String localize(String msgId, Object... messageParameters) {
		if (msgId != null) {
			String lang = getCurrentLocale().getISO3Language();
			if (this.resourceBundleRegistry.containsKey(lang)) {
				return this.resourceBundleRegistry.get(lang).renderMessage(msgId, messageParameters);
			} else if (msgId.matches(REGEX_TYPICAL_MESSAGE_ID)) {
				CottonUI.LOGGER.warn("Unable to localize '" + msgId + "'; no bundle for language '" + lang + "'.");
			}
			return msgId;
		} else {
			return null;
		}
	}

	// #########################################################################################################################################
	// ################################################################ LOG IN #################################################################
	// #########################################################################################################################################

	private enum UserChangeAllowance {
		ALLOW, REFRESH, DECLINE;
	}

	private UserChangeAllowance isUserChangeAllowed(UserChangeType userChangeType) {
		UserChangeAnnouncementEvent event = new UserChangeAnnouncementEvent(userChangeType);
		this.eventBus.dispatch(event, null);
		return event.doAccept() ? (event.doRefresh() ? UserChangeAllowance.REFRESH : UserChangeAllowance.ALLOW)
				: UserChangeAllowance.DECLINE;
	}

	private void notifyUserAwares(UserChangeType userChangeType) {
		this.eventBus.dispatch(new UserChangeEvent(userChangeType), null);
	}

	// ########## Internally Usable ##########

	final void showlogIn() {
		if (this.loginViewBlueprint == null) {
			throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
					"No default login view type has been configured at the UI, so auto login is not possible.");
		}
		appendToLog(SessionLogEntry.of(SessionLogContext.USER, SessionLogType.INFO, "Login shown."));
		setContent(this.injector.instantiate(this.loginViewBlueprint));
	}

	final boolean logIn(User user) {
		if (this.user != null) {
			throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
					"There already is a user logged in; perform a logout first.");
		}

		UserChangeAllowance allow = isUserChangeAllowed(UserChangeType.LOGIN);
		if (allow != UserChangeAllowance.DECLINE) {
			if (user == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Unable to log in a null user.");
			} else if (this.user != null) {
				throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
						"There is already a user logged in!");
			}
			CottonUI.LOGGER.info("User '" + this.user + "' logged in.");
			this.user = user;
			appendToLog(
					SessionLogEntry.of(SessionLogContext.USER, SessionLogType.INFO, "User '" + user + "' logged in."));
			if (allow == UserChangeAllowance.REFRESH) {
				refresh();
			}
			notifyUserAwares(UserChangeType.LOGIN);
		} else {
			appendToLog(SessionLogEntry.of(SessionLogContext.USER, SessionLogType.WARNING,
					"Login of user '" + user + "' denied."));
		}

		if (getContent() instanceof LoginView) {
			LoginView login = (LoginView) getContent();
			if (this.currentView != null
					&& this.currentView.getClass() == this.urlRegistry.getViewAt(this.currentUrl).getRootType()) {
				setContent(this.currentView);
			} else {
				refresh();
			}
			injector.destroy(login);
		}

		return allow == UserChangeAllowance.DECLINE;
	}

	final boolean logOut() {
		if (this.user != null) {
			UserChangeAllowance allow = isUserChangeAllowed(UserChangeType.LOGOUT);
			if (allow != UserChangeAllowance.DECLINE) {
				CottonUI.LOGGER.info("User '" + this.user + "' logged out.");
				this.user = null;
				appendToLog(SessionLogEntry.of(SessionLogContext.USER, SessionLogType.INFO,
						"User '" + user + "' logged out."));
				if (allow == UserChangeAllowance.REFRESH) {
					refresh();
				}
				notifyUserAwares(UserChangeType.LOGOUT);
				return true;
			} else {
				appendToLog(SessionLogEntry.of(SessionLogContext.USER, SessionLogType.WARNING,
						"Logout of user '" + user + "' denied."));
				return false;
			}
		} else {
			return false;
		}
	}

	final boolean isLoggedIn() {
		return this.user != null;
	}

	final User getLoggedInUser() {
		return this.user;
	}

	// #########################################################################################################################################
	// ################################################################ RIGHTS #################################################################
	// #########################################################################################################################################

	// ########## Internally Usable ##########

	final boolean areAllowed(Set<String> userRightIds) {
		if (userRightIds == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot check whether the current user has a null right.");
		}
		userRightIds = new HashSet<>(userRightIds);
		userRightIds.remove(null);

		if (userRightIds.isEmpty()) {
			return true;
		} else if (this.user != null) {
			appendToLog(SessionLogEntry.of(SessionLogContext.USER, SessionLogType.INFO, "Current user '" + this.user
					+ "' checked for the rights [" + StringUtils.join(userRightIds, ',') + "]"));
			return this.user.hasRights(userRightIds);
		} else {
			return false;
		}
	}

	// #########################################################################################################################################
	// ############################################################ ERROR HANDLING #############################################################
	// #########################################################################################################################################

	// ########## Internally Usable ##########

	final void appendToLog(SessionLogEntry... entries) {
		if (entries != null) {
			for (SessionLogEntry entry : entries) {
				if (entry != null) {
					log.add(entry);
				}
			}
		}
	}

	final boolean hasLogEntryOfContext(SessionLogContext context) {
		for (SessionLogEntry entry : this.log) {
			if (entry.getContext() == context) {
				return true;
			}
		}
		return false;
	}

	final boolean hasLogEntryOfType(SessionLogType type) {
		for (SessionLogEntry entry : this.log) {
			if (entry.getType() == type) {
				return true;
			}
		}
		return false;
	}

	final boolean hasLogEntryOfContextAndType(SessionLogContext context, SessionLogType type) {
		for (SessionLogEntry entry : this.log) {
			if (entry.getContext() == context && entry.getType() == type) {
				return true;
			}
		}
		return false;
	}

	final List<SessionLogEntry> getLog() {
		return Collections.unmodifiableList(this.log);
	}

	// ########## Externally Usable ##########

	private boolean isInternalErrorHandler = false;

	@Override
	public final void setErrorHandler(ErrorHandler errorHandler) {
		if (isInternalErrorHandler) {
			super.setErrorHandler(errorHandler);
		} else {
			throw new WebException(HttpErrorCodes.HTTP903_NOT_IMPLEMENTED_ERROR,
					"Own error handlers have to be set up during configuration phase of the "
							+ CottonServlet.class.getSimpleName() + ", using the given "
							+ CottonServlet.TemporalCottonServletConfiguration.class.getSimpleName() + "!");
		}
	}
}