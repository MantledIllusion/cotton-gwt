package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective;

import com.mantledillusion.vaadin.cotton.viewpresenter.Listen;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter;
import com.vaadin.ui.Button.ClickEvent;

public class NoComponentListenMethodPresenter extends Presenter<NoComponentListenMethodPresenterView> {
	
	@Listen("missingButtonComponent")
	private void listenMethod(ClickEvent event) {
		
	}
}