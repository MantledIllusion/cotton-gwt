package com.mantledillusion.vaadin.cotton.model;

import com.vaadin.data.HasValue;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Label;

class BindableLabel extends Label implements HasValue<String> {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean isReadOnly() {
		return true;
	}
	
	@Override
	public void setReadOnly(boolean readOnly) {
		// Do nothing - labels cannot be set non-readOnly
	}
	
	@Override
	public boolean isRequiredIndicatorVisible() {
		return false;
	}
	
	@Override
	public void setRequiredIndicatorVisible(boolean visible) {
		// Do nothing - labels cannot be used for input, so they cannot be required for input
	}

	@Override
	public Registration addValueChangeListener(ValueChangeListener<String> listener) {
		return ()->{};
	}
}
