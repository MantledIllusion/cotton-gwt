package com.mantledillusion.vaadin.cotton.model;

import com.vaadin.data.HasValue;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Label;

class BindableLabel extends Label implements HasValue<String> {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean isReadOnly() {
		return super.isReadOnly();
	}
	
	@Override
	public void setReadOnly(boolean readOnly) {
		super.setReadOnly(readOnly);
	}
	
	@Override
	public boolean isRequiredIndicatorVisible() {
		return super.isRequiredIndicatorVisible();
	}
	
	@Override
	public void setRequiredIndicatorVisible(boolean visible) {
		super.setRequiredIndicatorVisible(visible);
	}

	@Override
	public Registration addValueChangeListener(ValueChangeListener<String> listener) {
		return ()->{};
	}
}
