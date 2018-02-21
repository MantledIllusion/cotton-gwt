package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective;

import com.mantledillusion.vaadin.cotton.viewpresenter.Listen;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter;

public class MultiParamListenMethodPresenter extends Presenter<MultiParamListenMethodPresenterView> {
	
	@Listen
	private void multiParameterListenMethod(String event, String event2) {
		
	}
}