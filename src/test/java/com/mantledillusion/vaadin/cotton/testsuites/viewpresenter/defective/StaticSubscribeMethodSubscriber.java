package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe;

public class StaticSubscribeMethodSubscriber extends EventBusSubscriber {
	
	@Subscribe
	private static void subscribeMethod(BusEvent event) {
		
	}
}
