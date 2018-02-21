package com.mantledillusion.vaadin.cotton;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.mantledillusion.data.epiphy.index.PropertyIndex;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.annotation.Construct;
import com.mantledillusion.vaadin.cotton.model.IndexContext;
import com.mantledillusion.vaadin.cotton.testsuites.model.AbstractModelTestSuite;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.Model;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.TestModelAccessor;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.TestModelProperties;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.Sub;

public class ModelAccessTest {
	
	private static final class ModelAccessTestSuite extends AbstractModelTestSuite {
		
		@Construct
		private ModelAccessTestSuite() {
		}
	}
	
	private ModelAccessTestSuite suite;
	
	@Before
	public void setup() {
		this.suite = Injector.of().instantiate(ModelAccessTestSuite.class);
		this.suite.container.setModel(new Model());
	}

	@Test
	public void testModelGettingAndSetting() {
		assertTrue(this.suite.container.hasModel());
		this.suite.container.setModel(null);
		assertFalse(this.suite.container.hasModel());
		this.suite.container.setProperty(TestModelProperties.MODEL, new Model());
		assertTrue(this.suite.container.hasModel());
		this.suite.container.setProperty(TestModelProperties.MODEL, null);
		assertFalse(this.suite.container.hasModel());
	}

	@Test
	public void testPropertyGettingAndSetting() {
		assertEquals(null, (String) this.suite.container.getProperty(TestModelProperties.MODELID));
		this.suite.container.setProperty(TestModelProperties.MODELID, "testvalue");
		assertEquals("testvalue", this.suite.container.getProperty(TestModelProperties.MODELID));
	}
	
	@Test
	public void testListedPropertyAddingGettingSettingRemoving() {
		List<Sub> subList = new ArrayList<>();
		
		this.suite.container.setProperty(TestModelProperties.SUBLIST, subList);

		IndexContext context0 = IndexContext.of(PropertyIndex.of(TestModelProperties.SUBLIST, 0));
		this.suite.container.addProperty(TestModelProperties.SUBLIST, new Sub(), context0);
		this.suite.container.setProperty(TestModelProperties.SUBID, "testvalue0", context0);

		IndexContext context1 = IndexContext.of(PropertyIndex.of(TestModelProperties.SUBLIST, 1));
		this.suite.container.addProperty(TestModelProperties.SUBLIST, new Sub(), context1);
		this.suite.container.setProperty(TestModelProperties.SUBID, "testvalue1", context1);
		
		assertEquals(2, subList.size());
		
		TestModelAccessor sub1Accessor = this.suite.createIndexedAccessor(context1);
		
		assertEquals("testvalue1", this.suite.container.getProperty(TestModelProperties.SUBID, context1));
		assertEquals("testvalue1", sub1Accessor.getProperty(TestModelProperties.SUBID));
		
		this.suite.container.removeProperty(TestModelProperties.SUBLIST, context0);
		
		assertEquals(1, subList.size());
		
		assertEquals("testvalue1", this.suite.container.getProperty(TestModelProperties.SUBID, context0));
		assertEquals("testvalue1", sub1Accessor.getProperty(TestModelProperties.SUBID));
	}
}
