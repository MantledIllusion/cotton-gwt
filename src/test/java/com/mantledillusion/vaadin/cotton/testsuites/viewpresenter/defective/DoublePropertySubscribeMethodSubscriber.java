package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective;

import com.mantledillusion.vaadin.cotton.EventBusSubscriber;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe;
import com.mantledillusion.vaadin.cotton.viewpresenter.Subscribe.EventProperty;

public class DoublePropertySubscribeMethodSubscriber extends EventBusSubscriber {

	@Subscribe({@EventProperty(key="doubled", value="1"), @EventProperty(key="doubled", value="2")})
	private void subscribeMethod(BusEvent event) {
		
	}
}
