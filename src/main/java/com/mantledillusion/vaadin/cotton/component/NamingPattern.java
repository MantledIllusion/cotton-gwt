package com.mantledillusion.vaadin.cotton.component;

import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory.OptionPattern;
import com.vaadin.ui.AbstractComponent;

/**
 * Option pattern for {@link AbstractComponent} caption/description/id naming.
 */
public final class NamingPattern extends OptionPattern<AbstractComponent> {

	// ##############################################################################################################
	// ################################################ BUILDER #####################################################
	// ##############################################################################################################

	/**
	 * Builder for {@link NamingPattern}s.
	 */
	public static final class NamingPatternBuilder {

		private String captionMsgId;
		private Boolean captionAsHtml;
		private String descriptionMsgId;
		private String debugId;

		private NamingPatternBuilder() {
		}

		/**
		 * Sets the caption to the given one.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param captionMsgId
		 *            The caption (or localizable caption message id) to set; may be
		 *            null.
		 * @return this
		 */
		public NamingPatternBuilder andCaption(String captionMsgId) {
			this.captionMsgId = captionMsgId;
			return this;
		}

		/**
		 * Sets the caption to the given one, with the option to interpret the caption
		 * as HTML code.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param captionMsgId
		 *            The caption (or localizable caption message id) to set; may be
		 *            null.
		 * @param captionAsHtml
		 *            True if the given caption has to be interpreted as HTML code;
		 *            false otherwise.
		 * @return this
		 */
		public NamingPatternBuilder andCaption(String captionMsgId, boolean captionAsHtml) {
			this.captionMsgId = captionMsgId;
			this.captionAsHtml = captionAsHtml;
			return this;
		}

		/**
		 * Sets the description to the given one.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param descriptionMsgId
		 *            The description (or localizable description message id) to set;
		 *            may be null.
		 * @return this
		 */
		public NamingPatternBuilder andDescription(String descriptionMsgId) {
			this.descriptionMsgId = descriptionMsgId;
			return this;
		}

		/**
		 * Sets the debug id to the given value.
		 * 
		 * @param debugId
		 *            The debug id to set; may be null.
		 * @return this
		 */
		public NamingPatternBuilder andDebugId(String debugId) {
			this.debugId = debugId;
			return this;
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link NamingPattern} with the values currently set; never null
		 */
		public NamingPattern build() {
			return new NamingPattern(this.captionMsgId, this.captionAsHtml, this.descriptionMsgId, this.debugId);
		}
	}

	// ##############################################################################################################
	// ################################################## INIT ######################################################
	// ##############################################################################################################

	private final String caption;
	private final Boolean captionAsHtml;
	private final String description;
	private final String debugId;

	private NamingPattern(String caption, Boolean captionAsHtml, String description, String debugId) {
		this.caption = caption;
		this.captionAsHtml = captionAsHtml;
		this.description = description;
		this.debugId = debugId;
	}

	@Override
	void apply(AbstractComponent component) {
		if (this.captionAsHtml != null) {
			component.setCaptionAsHtml(this.captionAsHtml);
		}
		if (this.caption != null) {
			component.setCaption(WebEnv.localize(this.caption));
		}
		if (this.description != null) {
			component.setDescription(WebEnv.localize(this.description));
		}
		if (this.debugId != null) {
			component.setId(this.debugId);
		}
	}

	/**
	 * Directly builds a {@link NamingPattern} without using a builder.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * 
	 * @param captionMsgId
	 *            The caption (or localizable caption message id) to set; may be
	 *            null.
	 * @return A new {@link NamingPattern}; never null
	 */
	public static NamingPattern ofCaption(String captionMsgId) {
		return new NamingPattern(captionMsgId, null, null, null);
	}

	/**
	 * Starts a {@link NamingPatternBuilder} for building a {@link NamingPattern}.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * 
	 * @param captionMsgId
	 *            The caption (or localizable caption message id) to set; may be
	 *            null.
	 * @return A new {@link NamingPatternBuilder}; never null
	 */
	public static NamingPatternBuilder withCaption(String captionMsgId) {
		return new NamingPatternBuilder().andCaption(captionMsgId);
	}

	/**
	 * Directly builds a {@link NamingPattern} without using a builder.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * 
	 * @param captionMsgId
	 *            The caption (or localizable caption message id) to set; may be
	 *            null.
	 * @param captionAsHtml
	 *            True if the given caption has to be interpreted as HTML code;
	 *            false otherwise.
	 * @return A new {@link NamingPattern}; never null
	 */
	public static NamingPattern ofCaption(String captionMsgId, boolean captionAsHtml) {
		return new NamingPattern(captionMsgId, captionAsHtml, null, null);
	}

	/**
	 * Starts a {@link NamingPatternBuilder} for building a {@link NamingPattern}.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * 
	 * @param captionMsgId
	 *            The caption (or localizable caption message id) to set; may be
	 *            null.
	 * @param captionAsHtml
	 *            Whether to interpret the given caption as HTML code.
	 * @return A new {@link NamingPatternBuilder}; never null
	 */
	public static NamingPatternBuilder withCaption(String captionMsgId, boolean captionAsHtml) {
		return new NamingPatternBuilder().andCaption(captionMsgId, captionAsHtml);
	}

	/**
	 * Directly builds a {@link NamingPattern} without using a builder.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * 
	 * @param descriptionMsgId
	 *            The description (or localizable description message id) to set;
	 *            may be null.
	 * @return A new {@link NamingPattern}; never null
	 */
	public static NamingPattern ofDescription(String descriptionMsgId) {
		return new NamingPattern(null, null, descriptionMsgId, null);
	}

	/**
	 * Starts a {@link NamingPatternBuilder} for building a {@link NamingPattern}.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * 
	 * @param descriptionMsgId
	 *            The description (or localizable description message id) to set;
	 *            may be null.
	 * @return A new {@link NamingPatternBuilder}; never null
	 */
	public static NamingPatternBuilder withDescription(String descriptionMsgId) {
		return new NamingPatternBuilder().andDescription(descriptionMsgId);
	}

	/**
	 * Directly builds a {@link NamingPattern} without using a builder.
	 *
	 * @param debugId
	 *            The debug id to set; may be null.
	 * @return A new {@link NamingPattern}; never null
	 */
	public static NamingPattern ofDebugId(String debugId) {
		return new NamingPattern(null, null, null, debugId);
	}

	/**
	 * Starts a {@link NamingPatternBuilder} for building a {@link NamingPattern}.
	 *
	 * @param debugId
	 *            The debug id to set; may be null.
	 * @return A new {@link NamingPatternBuilder}; never null
	 */
	public static NamingPatternBuilder withDebugId(String debugId) {
		return new NamingPatternBuilder().andDebugId(debugId);
	}
}
