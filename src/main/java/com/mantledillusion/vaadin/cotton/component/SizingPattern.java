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

		private Integer width;
		private Unit widthUnit = Unit.PIXELS;
		private Integer height;
		private Unit heightUnit = Unit.PIXELS;

		private SizingPatternBuilder() {
		}

		/**
		 * Sets the width undefined.
		 * 
		 * @return this
		 */
		public SizingPatternBuilder andUndefinedWidth() {
			this.width = null;
			this.widthUnit = null;
			return this;
		}

		/**
		 * Sets the width to the given exact pixel value.
		 * 
		 * @param widthPx
		 *            The pixel value to set the width to.
		 * @return this
		 */
		public SizingPatternBuilder andExactWidth(int widthPx) {
			this.width = widthPx;
			this.widthUnit = Unit.PIXELS;
			return this;
		}

		/**
		 * Sets the width to 100%.
		 * 
		 * @return this
		 */
		public SizingPatternBuilder andFullWidth() {
			this.width = 100;
			this.widthUnit = Unit.PERCENTAGE;
			return this;
		}

		/**
		 * Sets the width to the given percental value.
		 * 
		 * @param widthPerc
		 *            The percental value to set the width to.
		 * @return this
		 */
		public SizingPatternBuilder andPercentalWidth(int widthPerc) {
			this.width = widthPerc;
			this.widthUnit = Unit.PERCENTAGE;
			return this;
		}

		/**
		 * Sets the height undefined.
		 * 
		 * @return this
		 */
		public SizingPatternBuilder andUndefinedHeight() {
			this.height = null;
			this.heightUnit = null;
			return this;
		}

		/**
		 * Sets the height to the given exact pixel value.
		 * 
		 * @param heightPx
		 *            The pixel value to set the height to.
		 * @return this
		 */
		public SizingPatternBuilder andExactHeight(int heightPx) {
			this.height = heightPx;
			this.heightUnit = Unit.PIXELS;
			return this;
		}

		/**
		 * Sets the height to 100%.
		 * 
		 * @return this
		 */
		public SizingPatternBuilder andFullHeight() {
			this.height = 100;
			this.heightUnit = Unit.PERCENTAGE;
			return this;
		}

		/**
		 * Sets the height to the given percental value.
		 * 
		 * @param heightPerc
		 *            The percental value to set the height to.
		 * @return this
		 */
		public SizingPatternBuilder andPercentalHeight(int heightPerc) {
			this.height = heightPerc;
			this.heightUnit = Unit.PERCENTAGE;
			return this;
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link NamingPattern} with the values currently set.
		 */
		public SizingPattern build() {
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

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPatternBuilder withUndefinedWidth() {
		return new SizingPatternBuilder().andUndefinedWidth();
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @param widthPx
	 *            The pixel value to set the width to.
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPattern ofExactWidth(int widthPx) {
		return new SizingPattern(widthPx, Unit.PIXELS, null, Unit.PIXELS);
	}

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @param widthPx
	 *            The pixel value to set the width to.
	 * @return A new {@link SizingPatternBuilder}; never null
	 */
	public static SizingPatternBuilder withExactWidth(int widthPx) {
		return new SizingPatternBuilder().andExactWidth(widthPx);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @return A new {@link SizingPattern} with 100% width; never null
	 */
	public static SizingPattern ofFullWidth() {
		return new SizingPattern(100, Unit.PERCENTAGE, null, Unit.PIXELS);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @param widthPerc
	 *            The percental value to set the width to.
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPattern ofPercentualWidth(int widthPerc) {
		return new SizingPattern(widthPerc, Unit.PERCENTAGE, null, Unit.PIXELS);
	}

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @return A new {@link SizingPatternBuilder} with width set to 100%; never null
	 */
	public static SizingPatternBuilder withFullWidth() {
		return new SizingPatternBuilder().andPercentalWidth(100);
	}

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @param widthPerc
	 *            The percental value to set the width to.
	 * @return A new {@link SizingPatternBuilder}; never null
	 */
	public static SizingPatternBuilder withPercentualWidth(int widthPerc) {
		return new SizingPatternBuilder().andPercentalWidth(widthPerc);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPatternBuilder withUndefinedHeight() {
		return new SizingPatternBuilder().andUndefinedHeight();
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @param heightPx
	 *            The pixel value to set the height to.
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPattern ofExactHeight(int heightPx) {
		return new SizingPattern(null, Unit.PIXELS, heightPx, Unit.PIXELS);
	}

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @param heightPx
	 *            The pixel value to set the height to.
	 * @return A new {@link SizingPatternBuilder}; never null
	 */
	public static SizingPatternBuilder withExactHeight(int heightPx) {
		return new SizingPatternBuilder().andExactHeight(heightPx);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @return A new {@link SizingPattern} with 100% height; never null
	 */
	public static SizingPattern ofFullHeight() {
		return new SizingPattern(null, Unit.PIXELS, 100, Unit.PERCENTAGE);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @param heightPerc
	 *            The percental value to set the height to.
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPattern ofPercentualHeight(int heightPerc) {
		return new SizingPattern(null, Unit.PIXELS, heightPerc, Unit.PERCENTAGE);
	}

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @return A new {@link SizingPatternBuilder} with height set to 100%; never
	 *         null
	 */
	public static SizingPatternBuilder withFullHeight() {
		return new SizingPatternBuilder().andPercentalHeight(100);
	}

	/**
	 * Starts a {@link SizingPatternBuilder} for building a {@link SizingPattern}.
	 * 
	 * @param heightPerc
	 *            The percental value to set the height to.
	 * @return A new {@link SizingPatternBuilder}; never null
	 */
	public static SizingPatternBuilder withPercentualHeight(int heightPerc) {
		return new SizingPatternBuilder().andPercentalHeight(heightPerc);
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPattern ofFullSize() {
		return new SizingPatternBuilder().andPercentalWidth(100).andPercentalHeight(100).build();
	}

	/**
	 * Directly builds a {@link SizingPattern} without using a builder.
	 * 
	 * @return A new {@link SizingPattern}; never null
	 */
	public static SizingPattern ofUndefinedSize() {
		return new SizingPatternBuilder().andUndefinedWidth().andUndefinedHeight().build();
	}
}