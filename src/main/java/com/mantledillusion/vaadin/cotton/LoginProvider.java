package com.mantledillusion.vaadin.cotton;

import com.mantledillusion.injection.hura.Blueprint;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.vaadin.cotton.environment.views.LoginView;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Restricted;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

/**
 * Provider for automated login.
 * <p>
 * Is triggered for example when {@link WebEnv#triggerlogIn()} is called or
 * a @{@link Restricted} {@link View} is visited.
 * <p>
 * Use one of the factory methods...<br>
 * - {@link #byView(Class)}<br>
 * - {@link #byView(TypedBlueprint)}<br>
 * - {@link #byUserProvider(Class)}<br>
 * - {@link #byUserProvider(TypedBlueprint)}<br>
 * ... to for instantiation.
 */
public abstract class LoginProvider {

	/**
	 * Provider for {@link User} instances when automated login is triggered.
	 */
	public interface UserProvider {

		/**
		 * Provides a {@link User} instance to login.
		 * 
		 * @return A {@link User} instance; never null.
		 */
		User provide();
	}

	private LoginProvider() {
	}

	abstract void login(Injector injector);

	/**
	 * Factory method for creating a {@link LoginProvider} using a
	 * {@link LoginView}.
	 * 
	 * @param loginViewType
	 *            The {@link LoginView} type to inject and use for login; might
	 *            <b>not</b> be null
	 * @return A new {@link LoginProvider}, never null
	 */
	public static final LoginProvider byView(Class<? extends LoginView> loginViewType) {
		return byView(Blueprint.of(loginViewType));
	}

	/**
	 * Factory method for creating a {@link LoginProvider} using a
	 * {@link LoginView}.
	 * 
	 * @param loginViewBlueprint
	 *            The {@link TypedBlueprint} to use to instantiate the
	 *            {@link LoginView} to use for login; might <b>not</b> be null
	 * @return A new {@link LoginProvider}, never null
	 */
	public static final LoginProvider byView(TypedBlueprint<? extends LoginView> loginViewBlueprint) {
		if (loginViewBlueprint == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot create a login provider with a null login view blueprint.");
		}
		return new LoginProvider() {

			@Override
			void login(Injector injector) {

				CottonUI.current().setContent(injector.instantiate(loginViewBlueprint));
			}
		};
	}

	/**
	 * Factory method for creating a {@link LoginProvider} using a
	 * {@link UserProvider}.
	 * 
	 * @param userProviderType
	 *            The {@link UserProvider} type to inject and use for login; might
	 *            <b>not</b> be null
	 * @return A new {@link LoginProvider}, never null
	 */
	public static final LoginProvider byUserProvider(Class<? extends UserProvider> userProviderType) {
		return byUserProvider(Blueprint.of(userProviderType));
	}

	/**
	 * Factory method for creating a {@link LoginProvider} using a
	 * {@link UserProvider}.
	 * 
	 * @param userProviderBlueprint
	 *            The {@link TypedBlueprint} to use to instantiate the
	 *            {@link UserProvider} to use for login; might <b>not</b> be null
	 * @return A new {@link LoginProvider}, never null
	 */
	public static final LoginProvider byUserProvider(TypedBlueprint<? extends UserProvider> userProviderBlueprint) {
		if (userProviderBlueprint == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot create a login provider with a null user provider blueprint.");
		}
		return new LoginProvider() {

			@Override
			void login(Injector injector) {
				CottonUI.current().logIn(injector.instantiate(userProviderBlueprint).provide());
				CottonUI.current().refresh();
			}
		};
	}
}
