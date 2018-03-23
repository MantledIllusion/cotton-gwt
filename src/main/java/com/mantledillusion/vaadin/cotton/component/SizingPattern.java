package com.mantledillusion.vaadin.cotton.component;

import com.mantledillusion.vaadin.cotton.component.ComponentFactory.OptionPattern;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.AbstractComponent;

/**
 * Option pattern for {@link AbstractComponent} sizing.
 */
public final class SizingPattern extends OptionPattern<AbstractComponent> {

	// ##############################################################################################################
	// ################################################ BUILDER #####################################################
	// ##############################################################################################################

	/**
	 * Builder for {@link SizingPattern}s.
	 */
	public static final class SizingPatternBuilder {

		private final Integer width;
		private final Unit widthUnit;
		private Integer height;
		private Unit heightUnit = Unit.PIXELS;

		private SizingPatternBuilder(Integer width, Unit widthUnit) {
			this.width = width;
			this.widthUnit = widthUnit;
		}

		/**
		 * Sets the height undefined.
		 * <p>
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link NamingPattern} with the values currently set.
		 */
		public SizingPattern andUndefinedHeight() {
			this.height = null;
			this.heightUnit = null;
			return build();
		}

		/**
		 * Sets the height to the given exact pixel value.
		 * <p>
		 * Builds a new pattern on every invocation.
		 * 
		 * @param heightPx
		 *            The pixel value to set the height to.
		 * @return A new {@link NamingPattern} with the values currently set.
		 */
		public SizingPattern andExactHeight(int heightPx) {
			this.height = heightPx;
			this.heightUnit = Unit.PIXELS;
			return build();
		}

		/**
		 * Sets the height to 100%.
		 * <p>
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link NamingPattern} with the values currently set.
		 */
		public SizingPattern andFullHeight() {
			this.height = 100;
			this.heightUnit = Unit.PERCENTAGE;
			return build();
		}

		/**
		 * Sets the height to the given percental value.
		 * <p>
		 * Builds a new pattern on every invocation.
		 * 
		 * @param heightPerc
		 *            The percental value to set the height to.
		 * @return A new {@link NamingPattern} with the values currently set.
		 */
		public SizingPattern andPercentalHeight(int heightPerc) {
			this.height = heightPerc;
			this.heightUnit = Unit.PERCENTAGE;
			return build();
		}

		private SizingPattern build() {
			return new SizingPattern(this.width, this.widthUnit, this.height, this.heightUnit);
		}
	}

	// ##############################################################################################################
	// ################################################## INIT ######################################################
	// ##############################################################################################################

	private final Integer width;
	private final Unit widthUnit;
	private final Integer height;
	private final Unit heightUnit;

	private SizingPattern(Integer width, Unit widthUnit, Integer height, Unit heightUnit) {
		this.width = width;
		this.widthUnit = widthUnit;
		this.height = height;
		this.heightUnit = heightUnit;
	}

	@Override
	void apply(AbstractComponent component) {
		if (this.width == null) {
			if (this.widthUnit == null) {
				component.setWidthUndefined();
			}
		} else {
			component.setWidth(this.width, this.widthUnit);
		}

		if (this.height == null) {
			if (this.heightUnit == null) {
				component.setHeightUndefined();
			}
		} else {
			component.setHeight(this.height, this.heightUnit);
		}
	}
	
	// UNDEFINED

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @return A new {@link SizingPattern} with width set as undefined; never null
	 */
	public static SizingPatternBuilder withUndefinedWidth() {
		return new SizingPatternBuilder(null, null);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @return A new {@link SizingPattern} with width and height set as undefined;
	 *         never null
	 */
	public static SizingPattern ofUndefinedSize() {
		return new SizingPatternBuilder(null, null).andUndefinedHeight();
	}

	// EXACT
	
	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @param widthPx
	 *            The pixel value to set the width to.
	 * @return A new {@link SizingPatternBuilder}; never null
	 */
	public static SizingPatternBuilder withExactWidth(int widthPx) {
		return new SizingPatternBuilder(widthPx, Unit.PIXELS);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @param widthPx
	 *            The pixel value to set the width to.
	 * @param heightPx
	 *            The pixel value to set the height to.
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPattern ofExactSize(int widthPx, int heightPx) {
		return new SizingPattern(widthPx, Unit.PIXELS, heightPx, Unit.PIXELS);
	}
	
	// PERCENTAL

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @return A new {@link SizingPatternBuilder} with width set to 100%; never null
	 */
	public static SizingPatternBuilder withFullWidth() {
		return new SizingPatternBuilder(100, Unit.PERCENTAGE);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @return A new {@link SizingPattern} with width and height set to 100%; never null
	 */
	public static SizingPattern ofFullSize() {
		return new SizingPattern(100, Unit.PERCENTAGE, 100, Unit.PERCENTAGE);
	}

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @param widthPerc
	 *            The percental value to set the width to.
	 * @return A new {@link SizingPatternBuilder}; never null
	 */
	public static SizingPatternBuilder withPercentualWidth(int widthPerc) {
		return new SizingPatternBuilder(widthPerc, Unit.PERCENTAGE);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @param widthPerc
	 *            The percental value to set the width to.
	 * @param heightPerc
	 *            The percental value to set the height to.
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPattern ofPercentualSize(int widthPerc, int heightPerc) {
		return new SizingPattern(widthPerc, Unit.PERCENTAGE, heightPerc, Unit.PERCENTAGE);
	}
}