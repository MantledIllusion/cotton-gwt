package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
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
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.ws.http.HTTPException;

import javax.servlet.http.Cookie;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Injector.RootInjector;
import com.mantledillusion.injection.hura.Predefinable;
import com.mantledillusion.injection.hura.Predefinable.Property;
import com.mantledillusion.injection.hura.Predefinable.Singleton;
import com.mantledillusion.injection.hura.annotation.Inject.SingletonMode;
import com.mantledillusion.vaadin.cotton.EventBusSubscriber.BusEvent;
import com.mantledillusion.vaadin.cotton.User.SessionLogContext;
import com.mantledillusion.vaadin.cotton.User.SessionLogEntry;
import com.mantledillusion.vaadin.cotton.User.SessionLogType;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Addressed;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.server.Page.PopStateEvent;
import com.vaadin.server.Page.PopStateListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.UI;

/**
 * UI base type.
 */
public abstract class CottonUI extends com.vaadin.ui.UI {

	private static final long serialVersionUID = 1L;
	static final Logger LOGGER = LoggerFactory.getLogger(CottonUI.class);

	private static final String REGEX_MESSAGE_ID_NAME_SEGMENT = "[^\\.\\s]+";
	static final String REGEX_TYPICAL_MESSAGE_ID = REGEX_MESSAGE_ID_NAME_SEGMENT + "(\\."
			+ REGEX_MESSAGE_ID_NAME_SEGMENT + ")+";
	private static final DateTimeFormatter COOKIE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy kk:mm:ss z");
	private static final String QUERY_PARAM_KEY_LANGUAGE = "lang";

	/**
	 * Temporarily active configuration type that can be used to configure a
	 * {@link CottonUI}.
	 * <P>
	 * May only be used during the configuration phase of the {@link CottonUI} it is
	 * given to.
	 */
	protected final class TemporalUIConfiguration {

		private boolean allowConfiguration = true;
		private List<Predefinable> predefinables = new ArrayList<>();

		private TemporalUIConfiguration() {
		}

		private void checkConfigurationAllowed() {
			if (!this.allowConfiguration) {
				throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
						"Configuration may only be done during the configuration phase of an UI.");
			}
		}

		/**
		 * Sets the default language of the {@link CottonUI} to the given
		 * {@link Locale}.
		 * <P>
		 * When a user visits the {@link CottonUI} without a language specified or with
		 * one where there is no {@link ResourceBundle} registered for, the language
		 * will automatically be switched to this default language.
		 * 
		 * @param locale
		 *            The {@link Locale} to set as default; <b>not</b> allowed to be
		 *            null.
		 */
		public void setDefaultLocale(Locale locale) {
			checkConfigurationAllowed();
			if (locale == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot set the default language to a null locale.");
			} else if (StringUtils.isBlank(locale.getISO3Language())) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot set the default language to a locale without an ISO3 language.");
			}
			CottonUI.this.defaultLang = locale.getISO3Language();
		}

		/**
		 * Uses combinations of the given base name and a single locale to create
		 * {@link ResourceBundle}s as the bundle to use when a user visits the
		 * {@link CottonUI} with the language set to a given {@link Locale}.
		 * <P>
		 * The String baseName+'_'+{@link Locale#getISO3Language()}+'.'+fileExtension
		 * will be used to look for a {@link Class} resource file.
		 * <P>
		 * Note that the default language is {@link Locale#US}, so if no localization is
		 * registered for {@link Locale#US} and the default language remains unchanged,
		 * there wont be any localization when the {@link CottonUI} displays content for
		 * the default language.
		 * 
		 * @param baseName
		 *            The base file name that should be used to build resource file
		 *            names; <b>not</b> allowed to be null or blank.
		 * @param fileExtension
		 *            The file extension that should be used to build resource file
		 *            names; <b>not</b> allowed to be null or blank.
		 * @param charset
		 *            The {@link Charset} to use to retrieve the resource file's
		 *            content, like 'UTF8' etc; <b>not</b> allowed to be null.
		 * @param locale
		 *            The first {@link Locale}s to find resource files for; <b>not</b>
		 *            allowed to be null.
		 * @param locales
		 *            Additional {@link Locale}s to find resource files for; might be
		 *            null, empty or contain nulls.
		 */
		public void registerLocalization(String baseName, String fileExtension, Charset charset, Locale locale,
				Locale... locales) {
			checkConfigurationAllowed();
			if (StringUtils.isBlank(baseName)) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a localization for a blank base name.");
			} else if (StringUtils.isBlank(fileExtension)) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a localization for a blank file extension.");
			} else if (charset == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a localization for a null charset.");
			} else if (locale == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a localization for a null first locale.");
			}
			LocalizationControl control = new LocalizationControl(charset, fileExtension);
			Set<Locale> uniqueLocales = new HashSet<>();
			uniqueLocales.add(locale);
			uniqueLocales.addAll(Arrays.asList(locales));
			uniqueLocales.remove(null);

			Set<Locale> addedLocales = new HashSet<>();
			Set<String> expectedBundleKeys = new HashSet<>();
			for (Locale loc : uniqueLocales) {
				if (loc != null) {
					if (StringUtils.isBlank(loc.getISO3Language())) {
						throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
								"Cannot register a localization to a locale with a blank ISO3 language.");
					}

					ResourceBundle bundle;
					try {
						bundle = ResourceBundle.getBundle(baseName, loc, control);
					} catch (MissingResourceException e) {
						throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
								"Unable to find localization class resource '" + baseName + '_' + loc.getISO3Language()
										+ '.' + fileExtension + "' for locale " + loc,
								e);
					}

					Set<String> bundleKeys = new HashSet<>(Collections.list(bundle.getKeys()));
					if (addedLocales.isEmpty()) {
						addedLocales.add(loc);
						expectedBundleKeys.addAll(bundleKeys);
					} else {
						Set<String> difference = SetUtils.disjunction(expectedBundleKeys, bundleKeys);
						if (difference.isEmpty()) {
							addedLocales.add(loc);
						} else {
							throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
									"The localization resource '" + baseName + '_' + loc.getISO3Language() + '.'
											+ fileExtension + "' for locale " + loc
											+ " differs from the resources of the already analyzed locales "
											+ addedLocales + " regarding the message ids " + difference
											+ "; on differently localed resources of the same base resource, all message id sets have to be equal.");
						}
					}

					if (!CottonUI.this.resourceBundleRegistry.containsKey(loc.getISO3Language())) {
						CottonUI.this.resourceBundleRegistry.put(loc.getISO3Language(), new LocalizationResource(loc));
					}
					CottonUI.this.resourceBundleRegistry.get(loc.getISO3Language()).addBundle(bundle, bundleKeys);
				}
			}
		}

		/**
		 * Registers a {@link TypedBlueprint} that can be used to instantiate an
		 * {@link LoginView} implementation that should be used for logins.
		 * <P>
		 * The given login view type will be instantiated when a login has to be done
		 * and should call {@link LoginView#logInAndReturn(User)} once the user has
		 * entered his credentials.
		 * 
		 * @param loginViewType
		 *            The login view type to set; might be null.
		 */
		public void registerLoginView(TypedBlueprint<? extends LoginView> loginViewType) {
			checkConfigurationAllowed();
			CottonUI.this.loginViewBlueprint = loginViewType;
		}

		/**
		 * Registers the given {@link ErrorView} type as the error handler for the given
		 * {@link Throwable} sub type.
		 * <P>
		 * This {@link Method} is shorthand for registering a
		 * {@link ErrorHandlingDecider} that returns the given {@link TypedBlueprint}
		 * for absolutely every error instance given to it.
		 * 
		 * @param <ErrorType>
		 *            The {@link Throwable} sub type to register an {@link ErrorView}
		 *            for.
		 * @param errorType
		 *            The {@link Throwable} sub type to register an {@link ErrorView}
		 *            for; <b>not</b> allowed to be null.
		 * @param viewBlueprint
		 *            The blueprint of the {@link ErrorView} type to use when the given
		 *            error type occurs; <b>not</b> allowed to be null.
		 */
		public <ErrorType extends Throwable> void registerErrorView(Class<ErrorType> errorType,
				TypedBlueprint<? extends ErrorView<ErrorType>> viewBlueprint) {
			checkConfigurationAllowed();
			if (viewBlueprint == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The error view type to register can never be null.");
			}
			registerErrorViewDecider(errorType, (error) -> viewBlueprint);
		}

		/**
		 * Registers the given {@link ErrorHandlingDecider} to be used to provide a
		 * {@link TypedBlueprint} to instantiate and inject an {@link ErrorView} from
		 * when an error of the given type occurs.
		 * <p>
		 * Note that if an instance of a further sub type of the given {@link Throwable}
		 * sub type occurs that does not have an own {@link ErrorHandlingDecider}
		 * registered, the given {@link ErrorHandlingDecider} will be used as fallback;
		 * for example, if an {@link ErrorHandlingDecider} is provided for
		 * {@link RuntimeException} and an {@link NullPointerException} occurs, the
		 * provider will be used anyway if there is no provider explicitly registered
		 * for {@link NullPointerException}.
		 * <P>
		 * That being said, registering an {@link ErrorHandlingDecider} for the
		 * {@link Throwable}.class will cause that provider to handle ALL occurring
		 * {@link Throwable}s that do not have a provider registered for a more
		 * specialized {@link Throwable} sub type, effectively creating a default error
		 * view provider.
		 * <P>
		 * Such a default error page provider exists internally by default, but can be
		 * overridden in the described way.
		 * 
		 * @param <ErrorType>
		 *            The {@link Throwable} sub type to register an
		 *            {@link ErrorHandlingDecider} for.
		 * @param errorType
		 *            The {@link Throwable} sub type to register an
		 *            {@link ErrorHandlingDecider} for; <b>not</b> allowed to be null.
		 * @param provider
		 *            The {@link ErrorHandlingDecider} to use upon an occurring error of
		 *            the given type; <b>not</b> allowed to be null.
		 */
		public <ErrorType extends Throwable> void registerErrorViewDecider(Class<ErrorType> errorType,
				ErrorHandlingDecider<ErrorType> provider) {
			if (errorType == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The error type to register an error view for can never be null.");
			} else if (provider == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The error view provider to register for an error type can never be null.");
			} else if (CottonUI.this.internalErrorHandler.hasDeciderRegisteredFor(errorType)) {
				throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
						"There is already an error view provider registered for the error type "
								+ errorType.getSimpleName());
			}
			CottonUI.this.internalErrorHandler.registerDecider(errorType, provider);
		}

		/**
		 * Sets the given {@link ErrorHandler} for handling errors of {@link Throwable}
		 * sub types that do not have a specialized {@link ErrorView} registered.
		 * <P>
		 * If there is no {@link ErrorHandler} set and an instance of a
		 * {@link Throwable} sub type occurs that does NOT have a specialized
		 * {@link ErrorView} registered, the default {@link ErrorView} is used for error
		 * displaying.
		 * 
		 * @param errorHandler
		 *            The {@link ErrorHandler} to set; might be null.
		 */
		public void setErrorHandler(ErrorHandler errorHandler) {
			checkConfigurationAllowed();
			CottonUI.this.internalErrorHandler.setExternalErrorHandler(errorHandler);
		}

		/**
		 * Registers the given {@link Predefinable}s (such as {@link Property}s or
		 * {@link SingletonMode#GLOBAL} {@link Singleton}s) to be available in every
		 * injected {@link View} (and its injected beans).
		 * 
		 * @param predefinables
		 *            The predefinables to register; might be null or contain nulls,
		 *            both is ignored.
		 */
		public void registerPredefinables(Predefinable... predefinables) {
			if (predefinables != null) {
				for (Predefinable predefinable : predefinables) {
					this.predefinables.add(predefinable);
				}
			}
		}
	}

	/**
	 * Base type for {@link BusEvent}s that get dispatched by the {@link CottonUI}.
	 */
	public static abstract class AbstractCottonEvent extends BusEvent {

		private AbstractCottonEvent() {
		}
	}

	/**
	 * {@link BusEvent} that is dispatched on {@link CottonUI} shutdown.
	 */
	public static final class ShutdownEvent extends AbstractCottonEvent {

		private ShutdownEvent() {
		}
	}

	// INJECTION
	private RootInjector injector = Injector.of(); // Intermediate injector until after configuration
	private EventBus eventBus = new EventBus();

	// CURRENT
	private String currentUrl;
	private Map<String, String[]> currentParams;
	private final Map<String, CookieInstance> currentCookies = new HashMap<>();
	private View currentView;

	// NAVIGATION
	private UrlResourceRegistry urlRegistry;

	// LOCALIZATION
	private String defaultLang = Locale.US.getISO3Language();
	private final Map<String, LocalizationResource> resourceBundleRegistry = new HashMap<>();

	// LOGIN
	private TypedBlueprint<? extends LoginView> loginViewBlueprint;
	private User user;

	// ERROR HANDLING
	private final InternalErrorHandler internalErrorHandler;
	private final List<SessionLogEntry> log = new ArrayList<>();

	/**
	 * {@link Constructor}.
	 */
	protected CottonUI() {
		this.internalErrorHandler = new InternalErrorHandler();
		this.isInternalErrorHandler = true;
		setErrorHandler(this.internalErrorHandler);
		this.isInternalErrorHandler = false;
	}

	@Override
	protected final void init(VaadinRequest request) {
		try {
			appendToLog(SessionLogEntry.of(SessionLogContext.SESSION, SessionLogType.INFO,
					"Initializing session '" + request.getWrappedSession().getId() + "'"));

			if (request.getCookies() != null) {
				for (Cookie cookie : request.getCookies()) {
					this.currentCookies.put(cookie.getName(), new CookieInstance(cookie.getValue(), cookie.getMaxAge()));
				}
			}

			Page.getCurrent().addPopStateListener(new PopStateListener() {

				private static final long serialVersionUID = 1L;

				@Override
				public void uriChanged(PopStateEvent event) {
					handlePathChanged(QueryParam.fromParamAppender(event.getPage().getLocation().getQuery()));
				}
			});

			TemporalUIConfiguration conf = new TemporalUIConfiguration();

			this.urlRegistry = configure(conf);
			conf.allowConfiguration = false;
			if (this.urlRegistry == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot initialize a UI using a null URL registry.");
			}

			Singleton eventBus = Singleton.of(EventBus.PRESENTER_EVENT_BUS_ID, this.eventBus);
			this.injector = Injector.of(ListUtils.union(conf.predefinables, Arrays.asList(eventBus)));

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
	public final void close() {
		appendToLog(SessionLogEntry.of(SessionLogContext.SESSION, SessionLogType.INFO,
				"Closing session '" + getSession().getSession().getId() + "'"));

		super.close();
		try {
			this.eventBus.dispatch(new ShutdownEvent(), null);
		} catch (Throwable t) {
			throw new WebException(HttpErrorCodes.HTTP500_INTERNAL_SERVER_ERROR, "The UI instance '" + this
					+ "' could not be shutdown correctly; an error occurred during notifying event bus subscribers of the shutdown.",
					t);
		} finally {
			this.injector.destroyInjector();
		}
	}

	/**
	 * Configures the {@link CottonUI} on startup using the given
	 * {@link TemporalUIConfiguration} and returns an {@link UrlResourceRegistry}
	 * that is used for URL-&gt;view mapping.
	 * <P>
	 * The given {@link TemporalUIConfiguration} instance may only be used during
	 * the call of this {@link Method}.
	 * 
	 * @param configuration
	 *            The {@link TemporalUIConfiguration} to use for configuration;
	 *            <b>not</b> allowed to be null.
	 * @return An {@link UrlResourceRegistry} instance; never null.
	 */
	protected abstract UrlResourceRegistry configure(TemporalUIConfiguration configuration);

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
		} else if (!params.equals(this.currentParams) && Arrays.equals(params.get(QUERY_PARAM_KEY_LANGUAGE),
				this.currentParams.get(QUERY_PARAM_KEY_LANGUAGE))) {
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
		return event.doAccept;
	}

	// ########## Internally Usable ##########

	/**
	 * The types of navigation changes that are possible.
	 */
	public static enum NavigationType {

		/**
		 * Change type of URL -&gt; another {@link Addressed}.
		 */
		SEGMENT_CHANGE,

		/**
		 * Change type of {@link QueryParam} value -&gt; new {@link QueryParam} value on
		 * the same URL.
		 */
		QUERY_PARAM_CHANGE,

		/**
		 * Refresh on the same URL.
		 */
		REFRESH;
	}

	/**
	 * The initiators of navigation changes that are possible.
	 */
	public static enum NavigationInitiator {

		/**
		 * The browser; manual navigation through back/forward, refresh or URl type in.
		 */
		BROWSER,

		/**
		 * The {@link CottonUI}; programmatic navigation through {@link WebEnv} or
		 * similar.
		 */
		UI;
	}

	/**
	 * {@link BusEvent} that is dispatched <b>before</b> a navigation is performed;
	 * therefore giving subscribers the possibility to {@link #decline()} the
	 * navigation.
	 * <p>
	 * Subscribers declining the navigation are expected to trigger user notifying
	 * mechanisms, so the user becomes aware why he could not navigate.
	 */
	public static final class NavigationAnnouncementEvent extends AbstractCottonEvent {

		private final NavigationType navigationType;
		private final NavigationInitiator navigationInitiator;
		private boolean doAccept = true;

		private NavigationAnnouncementEvent(NavigationType navigationType, NavigationInitiator navigationInitiator) {
			this.navigationType = navigationType;
			this.navigationInitiator = navigationInitiator;
		}

		/**
		 * Returns the type of navigation that is requested.
		 * 
		 * @return The {@link NavigationType}; never null
		 */
		public NavigationType getNavigationType() {
			return this.navigationType;
		}

		/**
		 * Returns the initiator of the requested navigation.
		 * 
		 * @return The {@link NavigationInitiator}; never null
		 */
		public NavigationInitiator getNavigationInitiator() {
			return navigationInitiator;
		}

		/**
		 * Marks the requested navigation to be declined.
		 */
		public void decline() {
			this.doAccept = false;
		}
	}

	/**
	 * {@link BusEvent} that is dispatched after a navigation has been performed.
	 */
	public static final class NavigationEvent extends AbstractCottonEvent {

		private final NavigationType navigationType;
		private final NavigationInitiator navigationInitiator;

		private NavigationEvent(NavigationType navigationType, NavigationInitiator navigationInitiator) {
			this.navigationType = navigationType;
			this.navigationInitiator = navigationInitiator;
		}

		/**
		 * Returns the initiator of the requested navigation.
		 * 
		 * @return The {@link NavigationInitiator}; never null
		 */
		public NavigationInitiator getNavigationInitiator() {
			return navigationInitiator;
		}

		/**
		 * Returns the type of navigation that has been performed.
		 * 
		 * @return The {@link NavigationType}; never null
		 */
		public NavigationType getNavigationType() {
			return this.navigationType;
		}
	}

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
		notifyNavigationAwares(NavigationType.QUERY_PARAM_CHANGE, NavigationInitiator.UI);
	}

	final void removeQueryParam(String key) {
		if (this.currentParams.containsKey(key)) {
			String[] values = currentParams.get(key);
			this.currentParams.remove(key);
			updateUrl(false);
			appendToLog(SessionLogEntry.of(SessionLogContext.NAVIGATION, SessionLogType.INFO,
					"Query param '" + key + "' with values [" + StringUtils.join(values, '/') + "] removed."));
			notifyNavigationAwares(NavigationType.QUERY_PARAM_CHANGE, NavigationInitiator.UI);
		}
	}

	final boolean navigateTo(NavigationTarget target) {
		return navigate(target.getUrl(), target.getParams(), true, true, NavigationInitiator.UI);
	}

	final boolean refresh() {
		return navigate(this.currentUrl, this.currentParams, true, false, NavigationInitiator.UI);
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

	/**
	 * Specialized {@link View} sub type that can be supplied to the
	 * {@link TemporalUIConfiguration} of a {@link CottonUI} during its
	 * configuration phase.
	 * <P>
	 * Whenever there is a need for a paged log in, the implementation of this
	 * {@link LoginView} configured there will be used automatically.
	 * <P>
	 * The view should log in the {@link User} it determines using its
	 * {@link #logInAndReturn(User)} {@link Method}.
	 */
	public static abstract class LoginView extends View {

		private static final long serialVersionUID = 1L;

		/**
		 * Should be used to log in a user instead of directly calling the
		 * {@link WebEnv}, as this {@link Method} is able to do a redirect to the last
		 * page opened before starting login process.
		 * 
		 * @param user
		 *            The {@link User} to log in; <b>not</b> allowed to be null.
		 */
		protected final void logInAndReturn(User user) {
			CottonUI ui = current();
			ui.logIn(user);
			if (ui.currentView != null
					&& ui.currentView.getClass() == ui.urlRegistry.getViewAt(ui.currentUrl).getRootType()) {
				ui.setContent(ui.currentView);
			} else {
				ui.refresh();
			}
			ui.injector.destroy(this);
		}
	}

	private enum UserChangeAllowance {
		ALLOW, REFRESH, DECLINE;
	}

	private UserChangeAllowance isUserChangeAllowed(CottonUI.UserChangeType userChangeType) {
		UserChangeAnnouncementEvent event = new UserChangeAnnouncementEvent(userChangeType);
		this.eventBus.dispatch(event, null);
		return event.doAccept ? (event.doRefresh ? UserChangeAllowance.REFRESH : UserChangeAllowance.ALLOW)
				: UserChangeAllowance.DECLINE;
	}

	private void notifyUserAwares(CottonUI.UserChangeType userChangeType) {
		this.eventBus.dispatch(new UserChangeEvent(userChangeType), null);
	}

	// ########## Internally Usable ##########

	/**
	 * The types of {@link User} changes that are possible.
	 */
	public static enum UserChangeType {

		/**
		 * Change type of no user before -&gt; new user after.
		 */
		LOGIN,

		/**
		 * Change type of old user before -&gt; no user after.
		 */
		LOGOUT;
	}

	/**
	 * {@link BusEvent} that is dispatched <b>before</b> a user change is performed;
	 * therefore giving subscribers the possibility to {@link #decline()} the user
	 * change.
	 * <p>
	 * Subscribers declining the user change are expected to trigger user notifying
	 * mechanisms, so the user becomes aware why the user change could not be
	 * performed.
	 */
	public static final class UserChangeAnnouncementEvent extends AbstractCottonEvent {

		private final UserChangeType changeType;
		private boolean doAccept = true;
		private boolean doRefresh = false;

		private UserChangeAnnouncementEvent(UserChangeType changeType) {
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
	}

	/**
	 * {@link BusEvent} that is dispatched after a user change has been performed.
	 */
	public static final class UserChangeEvent extends AbstractCottonEvent {

		private final UserChangeType changeType;

		private UserChangeEvent(UserChangeType changeType) {
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

		UserChangeAllowance allow = isUserChangeAllowed(CottonUI.UserChangeType.LOGIN);
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
			notifyUserAwares(CottonUI.UserChangeType.LOGIN);
			return true;
		} else {
			appendToLog(SessionLogEntry.of(SessionLogContext.USER, SessionLogType.WARNING,
					"Login of user '" + user + "' denied."));
			return false;
		}
	}

	final boolean logOut() {
		if (this.user != null) {
			UserChangeAllowance allow = isUserChangeAllowed(CottonUI.UserChangeType.LOGOUT);
			if (allow != UserChangeAllowance.DECLINE) {
				CottonUI.LOGGER.info("User '" + this.user + "' logged out.");
				this.user = null;
				appendToLog(SessionLogEntry.of(SessionLogContext.USER, SessionLogType.INFO,
						"User '" + user + "' logged out."));
				if (allow == UserChangeAllowance.REFRESH) {
					refresh();
				}
				notifyUserAwares(CottonUI.UserChangeType.LOGOUT);
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

	/**
	 * A decider for a specific error type.
	 * <p>
	 * The decider can either return an {@link ErrorView} {@link TypedBlueprint}
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
		 * error, it returns a {@link TypedBlueprint} that can be used to instantiate
		 * and inspect an instance of that view.
		 * <P>
		 * If it is not, it wraps the error in a {@link Throwable} and throws it to be
		 * handled by a different decider. For example, a provider could catch
		 * {@link HTTPException}s and wrap them into a {@link WebException}s with a
		 * matching matching {@link HttpErrorCodes}. That {@link WebException} would
		 * then be handled by a different {@link ErrorHandlingDecider} handling
		 * {@link WebException}s.
		 * <p>
		 * Note that if the {@link Throwable} implementing type is thrown more than once
		 * during handling an error it is assumed that the {@link ErrorHandlingDecider}s
		 * have run into a loop, causing the whole handling to be interupted with an
		 * {@link HttpErrorCodes#HTTP508_LOOP_DETECTED} {@link WebException} that is
		 * directly given to the underlaying {@link ErrorHandler}.
		 * 
		 * @param error
		 *            The caught error; might <b>not</b> be null.
		 * @return The {@link TypedBlueprint} to use for {@link ErrorView} retrieval
		 *         that can handle the given error; never null
		 * @throws Throwable
		 *             The {@link Throwable} that should be handled by some other
		 *             {@link ErrorHandlingDecider} instead of the given one
		 */
		TypedBlueprint<? extends ErrorView<ErrorType>> decide(ErrorType error) throws Throwable;
	}

	/**
	 * Specialized {@link View} sub type that can be supplied to the
	 * {@link TemporalUIConfiguration} of a {@link CottonUI} during its
	 * configuration phase.
	 *
	 * @param <ErrorType>
	 *            The {@link Throwable} sub type whose occurrences should be handled
	 *            by the {@link ErrorView} implementation.
	 */
	public static abstract class ErrorView<ErrorType extends Throwable> extends View {

		private static final long serialVersionUID = 1L;

		/**
		 * Will be called after the view has been instantiated for handling exactly one
		 * error of the {@link Throwable} sub type the {@link ErrorView} was registered
		 * for..
		 * 
		 * @param t
		 *            The error that occurred.
		 */
		protected abstract void handleError(ErrorType t);
	}

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

	/**
	 * Framework internal method <b>(DO NOT USE!)</b>
	 * <P>
	 * Use {@link CottonUI}'s configuration phase to configure an own
	 * {@link ErrorHandler} via
	 * {@link TemporalUIConfiguration#setErrorHandler(ErrorHandler)}.
	 */
	@Override
	public final void setErrorHandler(ErrorHandler errorHandler) {
		if (isInternalErrorHandler) {
			super.setErrorHandler(errorHandler);
		} else {
			throw new WebException(HttpErrorCodes.HTTP903_NOT_IMPLEMENTED_ERROR,
					"Own error handlers have to be set up during configuration phase of the UI, using the given "
							+ TemporalUIConfiguration.class.getSimpleName() + "!");
		}
	}
}
