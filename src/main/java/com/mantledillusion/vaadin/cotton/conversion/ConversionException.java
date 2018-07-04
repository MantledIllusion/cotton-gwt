package com.mantledillusion.vaadin.cotton.conversion;

public class ConversionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	ConversionException(Exception ex) {
		super(ex);
	}
}
