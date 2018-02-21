package com.mantledillusion.vaadin.cotton.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.mantledillusion.vaadin.cotton.component.ComponentFactory.OptionPattern;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractComponent;

/**
 * Option pattern for {@link AbstractComponent} icons and CSS style names.
 */
public final class StylingPattern extends OptionPattern<AbstractComponent> {

	// ##############################################################################################################
	// ################################################ BUILDER #####################################################
	// ##############################################################################################################

	/**
	 * Builder for {@link StylingPattern}s.
	 */
	public static final class StylingPatternBuilder {

		private Resource icon;
		private Set<String> styleNames = new HashSet<>();

		private StylingPatternBuilder() {
		}

		/**
		 * Sets the icon to the given one.
		 * 
		 * @param icon
		 *            The icon to use on the component; may be null, then nothing is
		 *            set.
		 * @return this
		 */
		public StylingPatternBuilder andIcon(Resource icon) {
			this.icon = icon;
			return this;
		}

		/**
		 * Adds the given style to the already included ones.
		 * 
		 * @param styleName
		 *            The CSS style name to use on the component; may <b>not</b> be
		 *            null.
		 * @return this
		 */
		public StylingPatternBuilder andStyle(String styleName) {
			if (styleName == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot add the style name null.");
			}
			this.styleNames.add(styleName);
			return this;
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link StylingPattern} with the values currently set; never
		 *         null
		 */
		public StylingPattern build() {
			return new StylingPattern(this.icon, Collections.unmodifiableSet(new HashSet<>(this.styleNames)));
		}
	}

	// ##############################################################################################################
	// ################################################## INIT ######################################################
	// ##############################################################################################################

	private final Resource icon;
	private final Set<String> styleNames;

	private StylingPattern(Resource icon, Set<String> styleNames) {
		this.icon = icon;
		this.styleNames = styleNames;
	}

	@Override
	void apply(AbstractComponent component) {
		if (this.icon != null) {
			component.setIcon(this.icon);
		}
		component.addStyleNames(this.styleNames.toArray(new String[this.styleNames.size()]));
	}

	/**
	 * Directly builds a {@link StylingPattern} without using a builder.
	 * 
	 * @param icon
	 *            The icon to use on the component; may be null, then nothing is
	 *            set.
	 * @return A new {@link StylingPattern}; never null
	 */
	public static StylingPattern ofIcon(Resource icon) {
		return new StylingPattern(icon, null);
	}

	/**
	 * Starts a {@link StylingPatternBuilder} for building a {@link StylingPattern}.
	 * 
	 * @param icon
	 *            The icon to use on the component; may be null, then nothing is
	 *            set.
	 * @return A new {@link StylingPatternBuilder}; never null
	 */
	public static StylingPatternBuilder withIcon(Resource icon) {
		return new StylingPatternBuilder().andIcon(icon);
	}

	/**
	 * Directly builds a {@link StylingPattern} without using a builder.
	 * 
	 * @param styleName
	 *            The CSS style name to use on the component; may <b>not</b> be
	 *            null.
	 * @return A new {@link StylingPattern}; never null
	 */
	public static StylingPattern ofStyle(String styleName) {
		if (styleName == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "Cannot add the style name null.");
		}
		return new StylingPattern(null, Collections.singleton(styleName));
	}

	/**
	 * Starts a {@link StylingPatternBuilder} for building a {@link StylingPattern}.
	 * 
	 * @param styleName
	 *            The CSS style name to use on the component; may <b>not</b> be
	 *            null.
	 * @return A new {@link StylingPatternBuilder}; never null
	 */
	public static StylingPatternBuilder withStyle(String styleName) {
		return new StylingPatternBuilder().andStyle(styleName);
	}
}