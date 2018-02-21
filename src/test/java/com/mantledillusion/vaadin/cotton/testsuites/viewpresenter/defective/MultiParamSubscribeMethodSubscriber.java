package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe;

public class MultiParamSubscribeMethodSubscriber extends EventBusSubscriber {

	@Subscribe
	private void subscribeMethod(BusEvent event, BusEvent event2) {
		
	}
}
