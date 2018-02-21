package com.mantledillusion.vaadin.cotton;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class CookieTest {

	private static final String[] UNALLOWED_COOKIE_VALUES = {"", "=", ";", ",", " ", "\t"};
	
	@Test
	public void testUnallowed() {
		for (String unallowed: UNALLOWED_COOKIE_VALUES) {
			assertFalse(unallowed.matches(Cookie.COOKIE_REGEX));
		}
	}
}
