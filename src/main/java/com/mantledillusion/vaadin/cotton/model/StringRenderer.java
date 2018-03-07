package com.mantledillusion.vaadin.cotton.model;

/**
 * Renderer for rendering a specific property type to Strings.
 * 
 * @param <PropertyType>
 *            The property type to render to String
 */
public interface StringRenderer<PropertyType> {

	/**
	 * A default {@link StringRenderer} that returns null for given null values or
	 * value.toString() otherwise.
	 */
	public static final StringRenderer<Object> DEFAULT_OBJECT_RENDERER = o -> o == null ? null : o.toString();

	/**
	 * Renders the given value as a String.
	 * 
	 * @param value
	 *            The value to render; might be null if the property to render is
	 *            null as well.
	 * @return A String that represents the given value; might be null
	 */
	public String render(PropertyType value);

	@SuppressWarnings("unchecked")
	public static <PropertyType> StringRenderer<PropertyType> defaultRenderer() {
		return (StringRenderer<PropertyType>) DEFAULT_OBJECT_RENDERER;
	}
}