package com.mantledillusion.vaadin.cotton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.Predefinable.Singleton;
import com.mantledillusion.injection.hura.annotation.Construct;
import com.mantledillusion.injection.hura.exception.InjectionException;
import com.mantledillusion.injection.hura.exception.ProcessorException;
import com.mantledillusion.injection.hura.exception.ValidatorException;
import com.mantledillusion.vaadin.cotton.EventBus;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.AbstractViewPresenterTestSuite;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective.DoublePropertySubscribeMethodSubscriber;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective.MultiParamListenMethodPresenterView;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective.MultiParamSubscribeMethodSubscriber;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective.NoComponentListenMethodPresenterView;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective.NoParamSubscribeMethodSubscriber;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective.StaticListenMethodPresenterView;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective.StaticSubscribeMethodSubscriber;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.defective.WrongParamSubscribeMethodSubscriber;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.PresenterA;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.PresenterB;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.PropertiedEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerAEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerAToTriggerBToTriggerAEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerBToTriggerAEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.TriggerBToTriggerUniversalEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.UniversalEvent;
import com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working.events.ViewAComponentFiredEvent;

public final class PresenterEventHandlingTest {

	private static final class PresenterEventhandlingTestSuite extends AbstractViewPresenterTestSuite {
	
		@Construct
		private PresenterEventhandlingTestSuite() {
		}
	}
	
	private PresenterEventhandlingTestSuite suite;
	
	@Before
	public void setup() {
		this.suite = Injector.of(Singleton.of(EventBus.PRESENTER_EVENT_BUS_ID, new EventBus())).instantiate(PresenterEventhandlingTestSuite.class);
	}
	
	@Test(expected=ProcessorException.class)
	public void testListenMethodChecking_Static() {
		this.suite.injectInSuiteContext(StaticListenMethodPresenterView.class);
	}
	
	@Test(expected=ProcessorException.class)
	public void testListenMethodChecking_MultiParam() {
		this.suite.injectInSuiteContext(MultiParamListenMethodPresenterView.class);
	}
	
	@Test(expected=InjectionException.class)
	public void testListenMethodChecking_NoComponent() {
		this.suite.injectInSuiteContext(NoComponentListenMethodPresenterView.class);
	}
	
	@Test
	public void testComponentListening() {
		suite.viewA.fireUnimportantEvent();
		suite.viewA.fireImportantEvent();

		assertEquals(1, suite.log.length());
		assertTrue(suite.log.isDispatched(0, PresenterA.class, ViewAComponentFiredEvent.class));
	}
	
	@Test(expected=ValidatorException.class)
	public void testSubscribeMethodChecking_Static() {
		this.suite.injectInSuiteContext(StaticSubscribeMethodSubscriber.class);
	}
	
	@Test(expected=ValidatorException.class)
	public void testSubscribeMethodChecking_NoParam() {
		this.suite.injectInSuiteContext(NoParamSubscribeMethodSubscriber.class);
	}
	
	@Test(expected=ValidatorException.class)
	public void testSubscribeMethodChecking_MultiParam() {
		this.suite.injectInSuiteContext(MultiParamSubscribeMethodSubscriber.class);
	}
	
	@Test(expected=ValidatorException.class)
	public void testSubscribeMethodChecking_WrongParam() {
		this.suite.injectInSuiteContext(WrongParamSubscribeMethodSubscriber.class);
	}
	
	@Test(expected=ProcessorException.class)
	public void testSubscribeMethodChecking_DoubleProperty() {
		this.suite.injectInSuiteContext(DoublePropertySubscribeMethodSubscriber.class);
	}
	
	@Test
	public void testBasicDispatchingAndSubscribing() {
		suite.dispatcher.dispatch(new TriggerAToTriggerBToTriggerAEvent());

		assertEquals(5, suite.log.length());
		assertTrue(suite.log.isReceival(0, PresenterA.class, TriggerAToTriggerBToTriggerAEvent.class));
		assertTrue(suite.log.isDispatched(1, PresenterA.class, TriggerBToTriggerAEvent.class));
		assertTrue(suite.log.isReceival(2, PresenterB.class, TriggerBToTriggerAEvent.class));
		assertTrue(suite.log.isDispatched(3, PresenterB.class, TriggerAEvent.class));
		assertTrue(suite.log.isReceival(4, PresenterA.class, TriggerAEvent.class));
	}
	
	@Test
	public void testPropertyExclusioning() {
		suite.dispatcher.dispatch(new PropertiedEvent("A"));

		assertEquals(1, suite.log.length());
		assertTrue(suite.log.isReceival(0, PresenterA.class, PropertiedEvent.class));
	}
	
	@Test
	public void testSelfObervancy() {
		suite.dispatcher.dispatch(new TriggerBToTriggerUniversalEvent());

		assertEquals(3, suite.log.length());
		assertTrue(suite.log.isReceival(0, PresenterB.class, TriggerBToTriggerUniversalEvent.class));
		assertTrue(suite.log.isDispatched(1, PresenterB.class, UniversalEvent.class));
		assertTrue(suite.log.isReceival(2, PresenterA.class, UniversalEvent.class));
	}
}
