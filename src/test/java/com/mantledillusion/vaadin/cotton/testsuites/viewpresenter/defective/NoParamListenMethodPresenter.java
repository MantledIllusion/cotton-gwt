package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective;

import com.mantledillusion.vaadin.cotton.viewpresenter.Listen;
import com.mantledillusion.vaadin.cotton.viewpresenter.Presenter;

public class NoParamListenMethodPresenter extends Presenter<NoParamListenMethodPresenterView> {

	@Listen
	private void handleBtnClicked() {
		
	}
}
