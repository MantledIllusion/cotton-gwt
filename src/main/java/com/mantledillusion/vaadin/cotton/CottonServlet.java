package com.mantledillusion.vaadin.cotton;

import com.mantledillusion.injection.hura.Blueprint;
import com.mantledillusion.injection.hura.Predefinable;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.injection.hura.annotation.Global.SingletonMode;
import com.mantledillusion.vaadin.cotton.environment.views.ErrorHandlingDecider;
import com.mantledillusion.vaadin.cotton.environment.views.ErrorView;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Addressed;
import com.mantledillusion.vaadin.cotton.viewpresenter.Restricted;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.*;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;

import javax.servlet.Servlet;

/**
 * {@link Servlet} that serves as Cotton's configuration.
 * <p>
 * Cotton is set up by extending this {@link CottonServlet} and overriding
 * {@link #configure(TemporalCottonServletConfiguration)}.
 * <p>
 * Cotton will use the URL path this {@link Servlet} is mapped on as the base
 * path of all @{@link Addressed} {@link View}s.
 */
public abstract class CottonServlet extends VaadinServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Temporarily active configuration type that can be used to configure a
	 * {@link CottonServlet}.
	 * <P>
	 * May only be used during the configuration phase of the {@link CottonServlet}
	 * it is given to.
	 */
	protected final class TemporalCottonServletConfiguration {

		private boolean allowConfiguration = true;

		// OPTIONS
		private String theme;
		private String widgetSet;
		private boolean preserveOnRefresh;
		private String pageTitle;
		private PushMode pushMode;
		private Transport transport;

		// NAVIGATION
		private final UrlResourceRegistry urlRegistry;

		// LOCALIZATION
		private String defaultLang = Locale.US.getISO3Language();
		private final Map<String, LocalizationResource> resourceBundleRegistry = new HashMap<>();

		// LOGIN
		private LoginProvider loginProvider;

		// ERROR HANDLING
		private final InternalErrorHandler internalErrorHandler;

		// BEANS
		private List<Predefinable> predefinables = new ArrayList<>();

		private TemporalCottonServletConfiguration() {
			this.urlRegistry = new UrlResourceRegistry();
			this.internalErrorHandler = new InternalErrorHandler();
		}

		private void checkConfigurationAllowed() {
			if (!this.allowConfiguration) {
				throw new WebException(WebException.HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
						"Configuration may only be done during the configuration phase of an UI.");
			}
		}

		/**
		 * Sets the name of the Vaadin theme to use.
		 * <p>
		 * Values set here have the same effect as using @{@link Theme} on an
		 * {@link UI}.
		 * 
		 * @param theme
		 *            The name of the theme to use; might be null
		 * @return this
		 */
		public TemporalCottonServletConfiguration setTheme(String theme) {
			this.theme = theme;
			return this;
		}

		/**
		 * Sets the name of the Vaadin widget set to use.
		 * <p>
		 * Values set here have the same effect as using @{@link Widgetset} on an
		 * {@link UI}.
		 * 
		 * @param widgetSet
		 *            The name of the widget set to use; might be null
		 * @return this
		 */
		public TemporalCottonServletConfiguration setWidgetSet(String widgetSet) {
			this.widgetSet = widgetSet;
			return this;
		}

		/**
		 * Sets whether to preserve the {@link UI} on refresh or not.
		 * <p>
		 * Values set here have the same effect as using @{@link PreserveOnRefresh} on
		 * an {@link UI}.
		 * 
		 * @param preserveOnRefresh
		 *            True if the {@link UI} should be preserved, false otherwise
		 * @return this
		 */
		public TemporalCottonServletConfiguration setPreserveOnRefresh(boolean preserveOnRefresh) {
			this.preserveOnRefresh = preserveOnRefresh;
			return this;
		}

		/**
		 * Sets the title of the {@link UI}.
		 * <p>
		 * Values set here have the same effect as using @{@link Title} on an
		 * {@link UI}.
		 * 
		 * @param pageTitle
		 *            The page title; might be null
		 * @return this
		 */
		public TemporalCottonServletConfiguration setPageTitle(String pageTitle) {
			this.pageTitle = pageTitle;
			return this;
		}

		/**
		 * Sets whether the {@link UI} should enable push mode and which transport to
		 * use.
		 * <p>
		 * Values set here have the same effect as using @{@link Push} on an {@link UI}.
		 * 
		 * @param pushMode
		 *            The {@link PushMode} the {@link UI} should use; might be null
		 * @param transport
		 *            The {@link Transport} the {@link UI} should use; might <b>not</b>
		 *            be null if push is enabled
		 * @return this
		 */
		public TemporalCottonServletConfiguration setPushMode(PushMode pushMode, Transport transport) {
			if (pushMode != null) {
				if (transport == null) {
					throw new IllegalArgumentException(
							"Cannot activate push in mode " + pushMode.name() + " for a null transport");
				}
			} else {
				transport = null;
			}
			this.pushMode = pushMode;
			this.transport = transport;
			return this;
		}

		/**
		 * Registers the given {@link View} implementation at the URL in the
		 * view's @{@link Addressed} annotation.
		 * 
		 * @param viewClass
		 *            The {@link View} implementation to register; might <b>not</b> be
		 *            null, also the has to be annotated with @{@link Addressed}
		 *            somewhere, view the documentation of {@link Addressed} for
		 *            reference.
		 * @return this
		 */
		public TemporalCottonServletConfiguration registerViewResource(Class<? extends View> viewClass) {
			this.urlRegistry.registerViewResource(viewClass);
			return this;
		}

		/**
		 * Registers the given {@link TypedBlueprint}'s root type {@link View}
		 * implementation at the URL in the view's @{@link Addressed} annotation.
		 * 
		 * @param viewBlueprint
		 *            The {@link TypedBlueprint} whose view to register; might
		 *            <b>not</b> be null, also the view has to be annotated
		 *            with @{@link Addressed} somewhere, view the documentation of
		 *            {@link Addressed} for reference.
		 * @return this
		 */
		public TemporalCottonServletConfiguration registerViewResource(TypedBlueprint<? extends View> viewBlueprint) {
			this.urlRegistry.registerViewResource(viewBlueprint);
			return this;
		}

		/**
		 * Returns whether there once was a view resource registered at the given URL,
		 * but there is no redirect to a new location so the server should throw a
		 * {@link WebException} with {@link HttpErrorCodes#HTTP410_GONE} when the URL is
		 * visited.
		 * 
		 * @param urlPath
		 *            Path to register the GONE resource at; might <b>not</b> be null
		 *            and has to match {@link WebUtils#URL_PATH_REGEX}
		 * @return this
		 */
		public TemporalCottonServletConfiguration registerGoneResource(String urlPath) {
			this.urlRegistry.registerGoneResource(urlPath);
			return this;
		}

		UrlResourceRegistry getUrlRegistry() {
			return urlRegistry;
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
		 * @return this
		 */
		public TemporalCottonServletConfiguration setDefaultLocale(Locale locale) {
			checkConfigurationAllowed();
			if (locale == null) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot set the default language to a null locale.");
			} else if (StringUtils.isBlank(locale.getISO3Language())) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot set the default language to a locale without an ISO3 language.");
			}
			this.defaultLang = locale.getISO3Language();
			return this;
		}

		String getDefaultLang() {
			return this.defaultLang;
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
		 * @return this
		 */
		public TemporalCottonServletConfiguration registerLocalization(String baseName, String fileExtension,
				Charset charset, Locale locale, Locale... locales) {
			checkConfigurationAllowed();
			if (StringUtils.isBlank(baseName)) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a localization for a blank base name.");
			} else if (StringUtils.isBlank(fileExtension)) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a localization for a blank file extension.");
			} else if (charset == null) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot register a localization for a null charset.");
			} else if (locale == null) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
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
						throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
								"Cannot register a localization to a locale with a blank ISO3 language.");
					}

					ResourceBundle bundle;
					try {
						bundle = ResourceBundle.getBundle(baseName, loc, control);
					} catch (MissingResourceException e) {
						throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
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
							throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
									"The localization resource '" + baseName + '_' + loc.getISO3Language() + '.'
											+ fileExtension + "' for locale " + loc
											+ " differs from the resources of the already analyzed locales "
											+ addedLocales + " regarding the message ids " + difference
											+ "; on differently localed resources of the same base resource, all message id sets have to be equal.");
						}
					}

					if (!this.resourceBundleRegistry.containsKey(loc.getISO3Language())) {
						this.resourceBundleRegistry.put(loc.getISO3Language(), new LocalizationResource(loc));
					}
					this.resourceBundleRegistry.get(loc.getISO3Language()).addBundle(bundle, bundleKeys);
				}
			}
			return this;
		}

		Map<String, LocalizationResource> getResourceBundleRegistry() {
			return resourceBundleRegistry;
		}

		/**
		 * Registers the given {@link LoginProvider} to be used for automatic login; for
		 * example when {@link WebEnv#triggerlogIn()} is called or a @{@link Restricted}
		 * {@link View} is visited.
		 * 
		 * @param loginProvider
		 *            The login provider to register; might be null
		 * @return this
		 */
		public TemporalCottonServletConfiguration registerLoginProvider(LoginProvider loginProvider) {
			checkConfigurationAllowed();
			this.loginProvider = loginProvider;
			return this;
		}

		LoginProvider getLoginProvider() {
			return loginProvider;
		}

		/**
		 * Registers the given {@link ErrorView} type as the error handler for the given
		 * {@link Exception} sub type.
		 * <P>
		 * This {@link Method} is shorthand for registering a
		 * {@link ErrorHandlingDecider} that returns the given
		 * {@link Blueprint.TypedBlueprint} for absolutely every error instance given to
		 * it.
		 *
		 * @param <ErrorType>
		 *            The {@link Exception} sub type to register an {@link ErrorView}
		 *            for.
		 * @param errorType
		 *            The {@link Exception} sub type to register an {@link ErrorView}
		 *            for; <b>not</b> allowed to be null.
		 * @param viewBlueprint
		 *            The blueprint of the {@link ErrorView} type to use when the given
		 *            error type occurs; <b>not</b> allowed to be null.
		 * @return this
		 */
		public <ErrorType extends Exception> TemporalCottonServletConfiguration registerErrorView(
				Class<ErrorType> errorType, Blueprint.TypedBlueprint<? extends ErrorView<ErrorType>> viewBlueprint) {
			checkConfigurationAllowed();
			if (viewBlueprint == null) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The error view type to register can never be null.");
			}
			registerErrorViewDecider(errorType, (error) -> viewBlueprint);
			return this;
		}

		/**
		 * Registers the given {@link ErrorHandlingDecider} to be used to provide a
		 * {@link Blueprint.TypedBlueprint} to instantiate and inject an
		 * {@link ErrorView} from when an error of the given type occurs.
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
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The error type to register an error view for can never be null.");
			} else if (provider == null) {
				throw new WebException(WebException.HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"The error view provider to register for an error type can never be null.");
			} else if (this.internalErrorHandler.hasDeciderRegisteredFor(errorType)) {
				throw new WebException(WebException.HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
						"There is already an error view provider registered for the error type "
								+ errorType.getSimpleName());
			}
			this.internalErrorHandler.registerDecider(errorType, provider);
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
		 * @return this
		 */
		public TemporalCottonServletConfiguration setErrorHandler(ErrorHandler errorHandler) {
			checkConfigurationAllowed();
			this.internalErrorHandler.setExternalErrorHandler(errorHandler);
			return this;
		}

		InternalErrorHandler getInternalErrorHandler() {
			return this.internalErrorHandler;
		}

		/**
		 * Registers the given {@link Predefinable}s (such as
		 * {@link Predefinable.Property}s or {@link SingletonMode#GLOBAL}
		 * {@link Predefinable.Singleton}s) to be available in every injected
		 * {@link View} (and its injected beans).
		 *
		 * @param predefinables
		 *            The predefinables to register; might be null or contain nulls,
		 *            both is ignored.
		 * @return this
		 */
		public TemporalCottonServletConfiguration registerPredefinables(Predefinable... predefinables) {
			if (predefinables != null) {
				for (Predefinable predefinable : predefinables) {
					this.predefinables.add(predefinable);
				}
			}
			return this;
		}

		List<Predefinable> getPredefinables() {
			return this.predefinables;
		}
	}

	@Override
	protected final VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration)
			throws ServiceException {
		VaadinServletService service = super.createServletService(deploymentConfiguration);

		TemporalCottonServletConfiguration config = new TemporalCottonServletConfiguration();
		configure(config);
		config.allowConfiguration = false;

		service.addSessionInitListener(
				sessionInitEvent -> sessionInitEvent.getSession().addUIProvider(new UIProvider() {

					private static final long serialVersionUID = 1L;

					@Override
					public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
						return CottonUI.class;
					}

					@Override
					public UI createInstance(UICreateEvent event) {
						return new CottonUI(config);
					}

					@Override
					public String getTheme(UICreateEvent event) {
						return config.theme;
					}

					@Override
					public WidgetsetInfo getWidgetsetInfo(UICreateEvent event) {
						if (config.widgetSet != null) {
							return new WidgetsetInfo() {

								private static final long serialVersionUID = 1L;

								@Override
								public boolean isCdn() {
									return false;
								}

								@Override
								public String getWidgetsetUrl() {
									return null;
								}

								@Override
								public String getWidgetsetName() {
									return config.widgetSet;
								}
							};
						} else {
							return super.getWidgetsetInfo(event);
						}
					}

					public boolean isPreservedOnRefresh(UICreateEvent event) {
						return config.preserveOnRefresh;
					}

					public String getPageTitle(UICreateEvent event) {
						return config.pageTitle;
					}

					public PushMode getPushMode(UICreateEvent event) {
						return config.pushMode;
					}

					public Transport getPushTransport(UICreateEvent event) {
						return config.transport;
					}
				}));
		return service;
	}

	/**
	 * Configures the {@link CottonServlet} on startup using the given
	 * {@link TemporalCottonServletConfiguration} and returns an
	 * {@link UrlResourceRegistry} that is used for URL-&gt;view mapping.
	 * <P>
	 * The given {@link TemporalCottonServletConfiguration} instance may only be
	 * used during the call of this {@link Method}.
	 *
	 * @param config
	 *            The {@link TemporalCottonServletConfiguration} to use for
	 *            configuration; <b>not</b> allowed to be null.
	 */
	protected abstract void configure(TemporalCottonServletConfiguration config);
}
