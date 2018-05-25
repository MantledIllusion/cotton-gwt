package com.mantledillusion.vaadin.cotton.model;

import com.vaadin.server.SerializableConsumer;

class ReadOnlyHasValue<V> extends com.vaadin.data.ReadOnlyHasValue<V> {

	private static final long serialVersionUID = 1L;

	ReadOnlyHasValue(SerializableConsumer<V> valueProcessor) {
		super(valueProcessor);
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		// This is always read only; ignore call
	}
}
