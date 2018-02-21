package com.mantledillusion.vaadin.cotton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mantledillusion.data.epiphy.index.PropertyIndex;
import com.mantledillusion.injection.hura.Injector;
import com.mantledillusion.injection.hura.annotation.Construct;
import com.mantledillusion.injection.hura.annotation.Process;
import com.mantledillusion.vaadin.cotton.model.ModelPersistor;
import com.mantledillusion.vaadin.cotton.model.IndexContext;
import com.mantledillusion.vaadin.cotton.testsuites.model.AbstractModelTestSuite;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.Model;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.TestModelAccessor;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.TestModelProperties;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.Sub;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.SubSub;

public final class ModelPersistorTest {
	
	private static final class ModelPersistorTestSuite extends AbstractModelTestSuite {
		
		Set<String> modelIds = new HashSet<>();
		Set<String> subIds = new HashSet<>();
		Set<String> subSubIds = new HashSet<>();
		
		@Construct
		private ModelPersistorTestSuite() {
		}

		@Process
		public void onWired() {
			
			new ModelPersistor<Model, Model>(container, TestModelProperties.MODEL) {

				@Override
				protected Model persistInstance(Model propertyInstance) throws Exception {
					modelIds.add(propertyInstance.modelId);
					return propertyInstance;
				}
			};
			
			new ModelPersistor<Model, Sub>(container, TestModelProperties.SUB) {

				@Override
				protected Sub persistInstance(Sub propertyInstance) throws Exception {
					subIds.add(propertyInstance.subId);
					return propertyInstance;
				}
			};
			
			new ModelPersistor<Model, SubSub>(container, TestModelProperties.SUBSUB) {

				@Override
				protected SubSub persistInstance(SubSub propertyInstance) throws Exception {
					subSubIds.add(propertyInstance.subSubId);
					return propertyInstance;
				}
			};
		}
	}
	
	private ModelPersistorTestSuite suite;
	
	@Before
	public void setup() {
		this.suite = Injector.of().instantiate(ModelPersistorTestSuite.class);
		
		Model model = new Model();
		model.subList = new ArrayList<>();

		model.subList.add(new Sub());
		model.subList.get(0).subId = "one";
		model.subList.get(0).subSubList = new ArrayList<>();
		
		model.subList.get(0).subSubList.add(new SubSub());
		model.subList.get(0).subSubList.get(0).subSubId = "oneone";
		
		model.subList.get(0).subSubList.add(new SubSub());
		model.subList.get(0).subSubList.get(1).subSubId = "onetwo";
		
		model.subList.add(new Sub());
		model.subList.get(1).subId = "two";
		model.subList.get(1).subSubList = new ArrayList<>();
		
		model.subList.get(1).subSubList.add(new SubSub());
		model.subList.get(1).subSubList.get(0).subSubId = "twoone";
		
		model.subList.get(1).subSubList.add(new SubSub());
		model.subList.get(1).subSubList.get(1).subSubId = "twotwo";
		
		this.suite.container.setModel(model);
	}

	@Test
	public void testModelChanged() {
		
		suite.container.setProperty(TestModelProperties.MODELID, "changedModelId");
		
		// CHECK CHANGES
		
		Assert.assertTrue(suite.container.isModelChanged());
		
		// PERSIST & CHECK

		suite.container.persist();
		
		Assert.assertTrue(suite.modelIds.equals(new HashSet<>(Arrays.asList("changedModelId"))) && suite.subIds.isEmpty() && suite.subSubIds.isEmpty());
		
		// CHECK CHANGES
		
		Assert.assertFalse(suite.container.isModelChanged());
	}

	@Test
	public void testSubSubChanged() {
		
		suite.container.setProperty(TestModelProperties.SUBSUBID, "changedSubSubId", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1)));
		
		// CHECK CHANGES
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
		
		// PERSIST & CHECK
		
		suite.container.persist();
		
		Assert.assertTrue(suite.modelIds.isEmpty() && suite.subIds.isEmpty() && suite.subSubIds.equals(new HashSet<>(Arrays.asList("changedSubSubId"))));
		
		// CHECK CHANGES
		
		Assert.assertFalse(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
	}

	@Test
	public void testBothSubSubChanged() {
		
		suite.container.setProperty(TestModelProperties.SUBSUBID, "changedSubSubIdOne", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0)));
		suite.container.setProperty(TestModelProperties.SUBSUBID, "changedSubSubIdTwo", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1)));
		
		// CHECK CHANGES
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0))));
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
		
		// PERSIST & CHECK
		
		suite.container.persist();
		
		Assert.assertTrue(suite.modelIds.isEmpty() && suite.subIds.isEmpty() && suite.subSubIds.equals(new HashSet<>(Arrays.asList("changedSubSubIdOne", "changedSubSubIdTwo"))));
		
		// CHECK CHANGES
		
		Assert.assertFalse(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0))));
		
		Assert.assertFalse(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
	}

	@Test
	public void testSubSubsOfDifferentSubsChanged() {
		
		suite.container.setProperty(TestModelProperties.SUBSUBID, "changedSubSubIdOne", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0)));
		suite.container.setProperty(TestModelProperties.SUBSUBID, "changedSubSubIdTwo", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 1), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1)));
		
		// CHECK CHANGES
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0))));
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 1), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
		
		// PERSIST & CHECK
		
		suite.container.persist();
		
		Assert.assertTrue(suite.modelIds.isEmpty() && suite.subIds.isEmpty() && suite.subSubIds.equals(new HashSet<>(Arrays.asList("changedSubSubIdOne", "changedSubSubIdTwo"))));
		
		// CHECK CHANGES
		
		Assert.assertFalse(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0))));
		
		Assert.assertFalse(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 1), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
	}

	@Test
	public void testSubAndSubSubChanged() {
		
		suite.container.setProperty(TestModelProperties.SUBID, "changedSubId", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0)));
		suite.container.setProperty(TestModelProperties.SUBSUBID, "changedSubSubId", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1)));
		
		// CHECK CHANGES
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0))));
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
		
		// PERSIST & CHECK
		
		suite.container.persist();
		
		Assert.assertTrue(suite.modelIds.isEmpty() && suite.subIds.equals(new HashSet<>(Arrays.asList("changedSubId"))) && suite.subSubIds.isEmpty());
		
		// CHECK CHANGES
		
		Assert.assertFalse(suite.container.isPropertyChanged(TestModelProperties.SUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0))));
		
		Assert.assertFalse(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
	}
	
	@Test
	public void testPartialPersisting() {
		
		suite.container.setProperty(TestModelProperties.SUBSUBID, "changedSubSubIdOne", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0)));
		suite.container.setProperty(TestModelProperties.SUBSUBID, "changedSubSubIdTwo", IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 1), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1)));
		
		// CHECK CHANGES
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0))));
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 1), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
		
		// PERSIST & CHECK
		
		TestModelAccessor indexedAccessor = suite.createIndexedAccessor(IndexContext.of(PropertyIndex.of(TestModelProperties.SUBLIST, 1)));
		indexedAccessor.persist();
		
		Assert.assertTrue(suite.modelIds.isEmpty() && suite.subIds.isEmpty() && suite.subSubIds.equals(new HashSet<>(Arrays.asList("changedSubSubIdTwo"))));
		
		// CHECK CHANGES
		
		Assert.assertTrue(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 0), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 0))));
		
		Assert.assertFalse(suite.container.isPropertyChanged(TestModelProperties.SUBSUBID, IndexContext.
				of(PropertyIndex.of(TestModelProperties.SUBLIST, 1), PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
	}
}
