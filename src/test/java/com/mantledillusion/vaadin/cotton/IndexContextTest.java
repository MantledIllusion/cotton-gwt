package com.mantledillusion.vaadin.cotton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import com.mantledillusion.data.epiphy.index.PropertyIndex;
import com.mantledillusion.vaadin.cotton.model.IndexContext;
import com.mantledillusion.vaadin.cotton.testsuites.model.working.TestModelProperties;

public class IndexContextTest {

	private IndexContext a;
	private IndexContext b;
	
	@Before
	public void before() {
		this.a = IndexContext.of(PropertyIndex.of(TestModelProperties.SUBLIST, 0));
		this.b = IndexContext.of(PropertyIndex.of(TestModelProperties.SUBLIST, 0),
				PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1));
	}
	
	@Test
	public void testGetIndex() {
		assertEquals((Integer) 0, this.a.indexOf(TestModelProperties.SUBLIST));
		assertEquals((Integer) null, this.a.indexOf(TestModelProperties.SUBSUBLIST));
	}
	
	@Test
	public void testContainsIndex() {
		assertTrue(this.a.contains(TestModelProperties.SUBLIST));
		assertFalse(this.a.contains(TestModelProperties.SUBSUBLIST));
	}
	
	@Test
	public void testContainsContext() {
		assertFalse(this.a.contains(this.b));
		assertTrue(this.b.contains(this.a));
	}
	
	@Test
	public void testGetSubContext() {
		assertTrue(this.a.equals(this.b.intersection(new HashSet<>(Arrays.asList(TestModelProperties.SUBLIST)))));
	}
	
	@Test
	public void testGetExtendedContextByIndex() {
		assertTrue(this.b.equals(this.a.union(PropertyIndex.of(TestModelProperties.SUBSUBLIST, 1))));
	}
	
	@Test
	public void testGetExtendedContextByContext() {
		IndexContext c = IndexContext.of(PropertyIndex.of(TestModelProperties.SUBLIST, 1));
		assertTrue(this.b.equals(c.union(this.b)));
	}
	
	@Test
	public void testGetUpdatedContext() {
		IndexContext c = IndexContext.of(PropertyIndex.of(TestModelProperties.SUBLIST, 0),
				PropertyIndex.of(TestModelProperties.SUBSUBLIST, 2));
		assertTrue(c.equals(this.b.update(TestModelProperties.SUBSUBLIST, 0, +1)));
	}
}
