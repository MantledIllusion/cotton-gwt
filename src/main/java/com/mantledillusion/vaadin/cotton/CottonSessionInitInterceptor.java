package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.Method;

import javax.servlet.annotation.WebServlet;

import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

/**
 * Interceptor that can be used when a specific {@link CottonUI} implementation
 * should be used for all sub paths of a base URL path instead of being used on
 * the server's root URL "/".
 * <p>
 * In such a case, an instance of this {@link CottonSessionInitInterceptor} has
 * be added to the {@link VaadinService} handling the {@link VaadinRequest} that
 * initializes the {@link VaadinSession}.
 * <p>
 * The easiest way to accomplish the task is extending {@link VaadinServlet} (or
 * any sub type of it). The servlet extension can then be annotated
 * with @{@link WebServlet}, where {@link WebServlet#value()} is set to the base
 * path with a variable end, like "/foo/bar/*".
 * <p>
 * Then, the servlet extension should override the {@link Method}
 * {@link VaadinServlet#createServletService(DeploymentConfiguration)} and call
 * {@link VaadinService#addSessionInitListener(SessionInitListener)} with an
 * instance of {@link CottonSessionInitInterceptor}.
 * <p>
 * When the {@link VaadinSession} is created and an {@link UI} is required, the
 * {@link VaadinService} will reach out for the {@link VaadinSession}'s
 * {@link UIProvider}s, that will now contain a provider that delivers exactly
 * the {@link CottonUI} implementation that was given to the
 * {@link CottonSessionInitInterceptor}.
 */
public final class CottonSessionInitInterceptor implements SessionInitListener {

	private static final long serialVersionUID = 1L;

	private final Class<? extends CottonUI> uiClass;

	public CottonSessionInitInterceptor(Class<? extends CottonUI> uiClass) {
		if (uiClass == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot intercept session initialization using a null cotton ui type.");
		}
		this.uiClass = uiClass;
	}

	@Override
	public void sessionInit(SessionInitEvent event) throws ServiceException {
		event.getSession().addUIProvider(new UIProvider() {

			private static final long serialVersionUID = 1L;

			@Override
			public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
				return CottonSessionInitInterceptor.this.uiClass;
			}
		});
	}
}
