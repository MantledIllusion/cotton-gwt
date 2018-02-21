package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective;

import com.mantledillusion.vaadin.cotton.viewpresenter.Listen;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter;
import com.mantledillusion.vaadin.cotton.viewpresenter.Listen.ActiveComponent;

public class NoComponentListenMethodPresenter extends Presenter<MultiParamListenMethodPresenterView> {
	
	@Listen(@ActiveComponent("missingComponent"))
	private void listenMethod() {
		
	}
}