package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective;

import com.mantledillusion.vaadin.cotton.component.ComponentFactory;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presented;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;
import com.vaadin.ui.Component;

@Presented(NoComponentListenMethodPresenter.class)
public class NoComponentListenMethodPresenterView extends View {

	private static final long serialVersionUID = 1L;

	@Override
	protected Component buildUI(TemporalActiveComponentRegistry reg) throws Exception {
		return ComponentFactory.buildButton();
	}
}