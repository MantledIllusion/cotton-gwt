package com.mantledillusion.vaadin.cotton;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.mantledillusion.injection.hura.Blueprint;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.injection.hura.annotation.Construct;
import com.mantledillusion.vaadin.cotton.CottonUI.ErrorView;
import com.mantledillusion.vaadin.cotton.User.SessionLogContext;
import com.mantledillusion.vaadin.cotton.User.SessionLogEntry;
import com.mantledillusion.vaadin.cotton.User.SessionLogType;
import com.mantledillusion.vaadin.cotton.CottonUI.ErrorHandlingDecider;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.ErrorMessage;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

final class InternalErrorHandler implements ErrorHandler {

	private static final long serialVersionUID = 1L;

	private static final class DefaultErrorView extends CottonUI.ErrorView<Throwable> {

		private static final long serialVersionUID = 1L;

		private Label errorTitle;
		private Label errorLabel;
		private TextArea errorText;

		@Construct
		private DefaultErrorView() {
		}

		@Override
		protected Component buildUI(TemporalActiveComponentRegistry reg) throws Exception {
			HorizontalLayout layout = new HorizontalLayout();
			layout.setSizeFull();
			layout.setMargin(true);

			VerticalLayout column = new VerticalLayout();
			column.setWidth(66, Unit.PERCENTAGE);
			column.setHeightUndefined();
			layout.addComponent(column);
			layout.setComponentAlignment(column, Alignment.TOP_CENTER);

			this.errorTitle = new Label(null, ContentMode.HTML);
			this.errorTitle.setSizeUndefined();
			column.addComponent(this.errorTitle);
			column.setExpandRatio(this.errorTitle, 0);

			this.errorLabel = new Label(null, ContentMode.HTML);
			this.errorLabel.setWidth(100, Unit.PERCENTAGE);
			this.errorLabel.setHeightUndefined();
			column.addComponent(this.errorLabel);
			column.setExpandRatio(this.errorLabel, 0);

			this.errorText = new TextArea();
			this.errorText.setWidth(100, Unit.PERCENTAGE);
			this.errorText.setHeight(300, Unit.PIXELS);
			column.addComponent(errorText);
			column.setExpandRatio(errorText, 1);

			return layout;
		}

		@Override
		protected void handleError(Throwable t) {
			this.errorTitle.setValue("<b><font size=\"5\">" + t.getClass().getSimpleName() + ":</font></b>");
			this.errorLabel.setValue("<font size=\"4\">" + t.getLocalizedMessage() + "</font>");

			this.errorText.setValue(ExceptionUtils.getStackTrace(t));
			this.errorText.setComponentError(new ErrorMessage() {

				private static final long serialVersionUID = 1L;

				@Override
				public String getFormattedHtmlMessage() {
					return StringUtils.EMPTY;
				}

				@Override
				public ErrorLevel getErrorLevel() {
					return ErrorLevel.SYSTEM;
				}
			});
			this.errorText.setReadOnly(true);
		}
	}

	private final Map<Class<? extends Throwable>, ErrorHandlingDecider<?>> errorNavigationRegistry = new HashMap<>();
	private ErrorHandler externalErrorHandler;

	boolean hasDeciderRegisteredFor(Class<? extends Throwable> errorType) {
		return this.errorNavigationRegistry.containsKey(errorType);
	}

	<ErrorType extends Throwable> void registerDecider(Class<ErrorType> errorType,
			ErrorHandlingDecider<ErrorType> decider) {
		this.errorNavigationRegistry.put(errorType, decider);
	}

	void setExternalErrorHandler(ErrorHandler externalErrorHandler) {
		this.externalErrorHandler = externalErrorHandler;
	}

	@Override
	public void error(com.vaadin.server.ErrorEvent event) {
		for (Window window : CottonUI.current().getWindows()) {
			try {
				window.close();
			} catch (Exception e) {
				// Ignore; the window is closed anyway
			}
		}

		Throwable t = DefaultErrorHandler.findRelevantThrowable(event.getThrowable());

		CottonUI.current().appendToLog(
				SessionLogEntry.of(SessionLogContext.ACTION, SessionLogType.ERROR, "Handling error '" + t + "'"));

		LinkedHashMap<Class<? extends Throwable>, ErrorHandlingDecider<? extends Throwable>> handlingChain = new LinkedHashMap<>();
		handlingChain.put(t.getClass(), null);
		decideOrFallback(event, t, handlingChain);
	}

	private <ErrorType extends Throwable> void decideOrFallback(com.vaadin.server.ErrorEvent event, ErrorType error,
			LinkedHashMap<Class<? extends Throwable>, ErrorHandlingDecider<? extends Throwable>> handlingChain) {
		if (!decide(event, error, handlingChain)) {
			useFallback(event, error);
		}
	}

	private void useFallback(com.vaadin.server.ErrorEvent event, Throwable t) {
		if (this.externalErrorHandler != null) {
			try {
				this.externalErrorHandler.error(event);
			} catch (Throwable t2) {
				doDisplay(Blueprint.of(InternalErrorHandler.DefaultErrorView.class),
						new WebException(HttpErrorCodes.HTTP500_INTERNAL_SERVER_ERROR,
								"The error handler '" + this.externalErrorHandler.getClass().getSimpleName()
										+ "' threw an exception when called to handle the error '" + t + "': " + t2,
								t2));
			}
		} else {
			doDisplay(Blueprint.of(InternalErrorHandler.DefaultErrorView.class), t);
		}
	}

	@SuppressWarnings("unchecked")
	private <ErrorType extends Throwable, ViewType extends ErrorView<ErrorType>> boolean decide(
			com.vaadin.server.ErrorEvent event, ErrorType error,
			LinkedHashMap<Class<? extends Throwable>, ErrorHandlingDecider<? extends Throwable>> handlingChain) {
		Class<?> throwableType = error.getClass();

		while (Throwable.class.isAssignableFrom(throwableType)) {
			if (this.errorNavigationRegistry.containsKey(throwableType)) {
				ErrorHandlingDecider<ErrorType> decider = (ErrorHandlingDecider<ErrorType>) this.errorNavigationRegistry
						.get(throwableType);
				TypedBlueprint<? extends ErrorView<ErrorType>> blueprint = null;
				try {
					blueprint = decider.decide(error);
				} catch (Throwable t) {
					if (handlingChain.containsKey(t.getClass())) {
						handlingChain.put(t.getClass(), decider);
						useFallback(event, new WebException(HttpErrorCodes.HTTP508_LOOP_DETECTED,
								"A loop in error deciding was detected: " + toDisplayableString(handlingChain)));
					}
					handlingChain.put(t.getClass(), decider);
					decideOrFallback(new com.vaadin.server.ErrorEvent(t), t, handlingChain);
				}
				if (blueprint == null) {
					useFallback(event,
							new WebException(HttpErrorCodes.HTTP500_INTERNAL_SERVER_ERROR,
									"The decider '" + decider.getClass().getSimpleName() + "' returned a null "
											+ TypedBlueprint.class.getSimpleName() + " for the error '" + error
											+ "' which is forbidden.",
									error));
				}
				doDisplay(blueprint, error);
				return true;
			}
			throwableType = throwableType.getSuperclass();
		}
		return false;
	}

	private String toDisplayableString(
			LinkedHashMap<Class<? extends Throwable>, ErrorHandlingDecider<? extends Throwable>> handlingChain) {
		List<String> chain = handlingChain.entrySet().stream()
				.map(entry -> (entry.getKey().getSimpleName()
						+ (entry.getValue() != null ? " (thrown by " + entry.getValue() + ")" : StringUtils.EMPTY)))
				.collect(Collectors.toList());
		return StringUtils.join(chain, " -> ");

	}

	private <ErrorType extends Throwable, ViewType extends ErrorView<ErrorType>> void doDisplay(
			TypedBlueprint<ViewType> viewBlueprint, ErrorType error) {
		ViewType errorView = CottonUI.current().doDisplay(viewBlueprint);
		errorView.handleError(error);
		CottonUI.LOGGER.error("Showed error view of instance " + viewBlueprint.getRootType().getSimpleName()
				+ " for error of type " + error.getClass().getSimpleName(), error);
	}
}