package com.mantledillusion.vaadin.cotton.component;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

import org.apache.commons.lang3.StringUtils;

import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.component.ComponentFactory.OptionPattern;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.shared.ui.datefield.DateResolution;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractDateField;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.InlineDateField;
import com.vaadin.ui.InlineDateTimeField;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Option pattern for {@link AbstractComponent} individual sub-type options.
 */
public abstract class PresetPattern<T extends AbstractComponent> extends OptionPattern<T> {

	// ##############################################################################################################
	// ################################################ BUILDER #####################################################
	// ##############################################################################################################

	/**
	 * Base type for {@link PresetPattern} builders.
	 * 
	 * @param <T>
	 *            The {@link AbstractComponent} implementing type a build preset
	 *            pattern can be applied on.
	 */
	public interface PresetBuilder<T extends AbstractComponent> {

		/**
		 * Builds a {@link PresetPattern} of the {@link AbstractComponent} implementing
		 * type this builder uses.
		 * 
		 * @return A new {@link PresetPattern} instance; never null
		 */
		public PresetPattern<T> build();
	}

	/**
	 * {@link PresetBuilder} implementation for {@link PresetPattern}s that can be
	 * applied on {@link AbstractOrderedLayout}s.
	 * <P>
	 * Possible targets for the build {@link PresetPattern} are:<br>
	 * - {@link FormLayout}<br>
	 * - {@link HorizontalLayout}<br>
	 * - {@link VerticalLayout}
	 */
	public static final class OrderedLayoutPresetBuilder implements PresetBuilder<AbstractOrderedLayout> {

		private Boolean spacing;
		private Boolean margin;

		private OrderedLayoutPresetBuilder() {
		}

		/**
		 * Instruct the {@link AbstractOrderedLayout} to use spacing between components.
		 * 
		 * @return this
		 */
		public OrderedLayoutPresetBuilder withSpacing() {
			this.spacing = true;
			return this;
		}

		/**
		 * Instruct the {@link AbstractOrderedLayout} to use spacing between components,
		 * but no margin at its bounds.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		public PresetPattern<AbstractOrderedLayout> withSpacingOnly() {
			this.spacing = true;
			this.margin = false;
			return build();
		}

		/**
		 * Instruct the {@link AbstractOrderedLayout} to not use spacing between
		 * components.
		 * 
		 * @return this
		 */
		public OrderedLayoutPresetBuilder withoutSpacing() {
			this.spacing = false;
			return this;
		}

		/**
		 * Instruct the {@link AbstractOrderedLayout} to use a margin at its bounds.
		 * 
		 * @return this
		 */
		public OrderedLayoutPresetBuilder withMargin() {
			this.margin = true;
			return this;
		}

		/**
		 * Instruct the {@link AbstractOrderedLayout} to use a margin at its bounds, but
		 * no spacing between components.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		public PresetPattern<AbstractOrderedLayout> withMarginOnly() {
			this.spacing = false;
			this.margin = true;
			return build();
		}

		/**
		 * Instruct the {@link AbstractOrderedLayout} to not use a margin at its bounds.
		 * 
		 * @return this
		 */
		public OrderedLayoutPresetBuilder withoutMargin() {
			this.margin = false;
			return this;
		}

		/**
		 * Instruct the {@link AbstractOrderedLayout} to use a margin at its bounds and
		 * spacing between components.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		public PresetPattern<AbstractOrderedLayout> withAllGaps() {
			this.spacing = true;
			this.margin = true;
			return build();
		}

		/**
		 * Instruct the {@link AbstractOrderedLayout} to neither use a margin at its
		 * bounds, nor spacing between components.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		public PresetPattern<AbstractOrderedLayout> withoutAnyGap() {
			this.spacing = false;
			this.margin = false;
			return build();
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		@Override
		public PresetPattern<AbstractOrderedLayout> build() {
			return new PresetPattern<AbstractOrderedLayout>() {

				private final Boolean spacing = OrderedLayoutPresetBuilder.this.spacing;
				private final Boolean margin = OrderedLayoutPresetBuilder.this.margin;

				@Override
				public void apply(AbstractOrderedLayout component) {
					if (this.spacing != null) {
						component.setSpacing(this.spacing);
					}
					if (this.margin != null) {
						component.setMargin(this.margin);
					}
				}
			};
		}
	}

	/**
	 * Base type for {@link PresetBuilder}s for {@link AbstractDateField}s.
	 *
	 * @param <B>
	 * @param <T>
	 * @param <R>
	 */
	public static abstract class TemporalFieldPresetBuilder<B extends TemporalFieldPresetBuilder<B, T, R>, T extends Temporal & TemporalAdjuster & Serializable & Comparable<? super T>, R extends Enum<R>>
			implements PresetBuilder<AbstractDateField<T, R>> {

		private String dateFormat;
		private String dateOutOfRangeMessage;
		private String dateUnparsableMessage;
		private Boolean readOnly;

		private T rangeStart;
		private T rangeEnd;
		private R resolution;
		private ZoneId zoneId;

		private TemporalFieldPresetBuilder() {
		}

		protected abstract B getThis();

		/**
		 * Sets the date format to the given value.
		 * 
		 * @param dateFormat
		 *            The date format to use on the {@link AbstractDateField}, see
		 *            {@link SimpleDateFormat} for details; may <b>not</b> be null.
		 * @return this
		 */
		public B withFormat(String dateFormat) {
			if (dateFormat == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Unable to create a date field with a null format.");
			}
			this.dateFormat = dateFormat;
			return getThis();
		}

		/**
		 * Sets a message to use when the value of the {@link AbstractDateField} is out
		 * of the range set to the field.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param dateOutOfRangeMsgId
		 *            The message (or localizable message id) to set; may be null.
		 * @return this
		 */
		public B withMessageForOutOfRangeValue(String dateOutOfRangeMsgId) {
			this.dateOutOfRangeMessage = dateOutOfRangeMsgId;
			return getThis();
		}

		/**
		 * Removes the use of a message when the value of the {@link AbstractDateField}
		 * is out of the range set to the field.
		 * 
		 * @return this
		 */
		public B withoutMessageForOutOfRangeValue() {
			this.dateOutOfRangeMessage = null;
			return getThis();
		}

		/**
		 * Sets a message to use when the value of the {@link AbstractDateField} is not
		 * parsable by the date format set to the field.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param dateUnparsableMessage
		 *            The message (or localizable message id) to set; may be null.
		 * @return this
		 */
		public B withMessageForUnparsableValue(String dateUnparsableMessage) {
			this.dateUnparsableMessage = dateUnparsableMessage;
			return getThis();
		}

		/**
		 * Removes the use of a message when the value of the {@link AbstractDateField}
		 * is not parsable by the date format set to the field.
		 * 
		 * @return this
		 */
		public B withoutMessageForUnparsableValue() {
			this.dateUnparsableMessage = null;
			return getThis();
		}

		/**
		 * Sets the field to be read only; no changes to the value by the user are
		 * allowed.
		 * 
		 * @return this
		 */
		public B withReadOnly() {
			this.readOnly = true;
			return getThis();
		}

		/**
		 * Sets the field's value to be changeable by the user.
		 * 
		 * @return this
		 */
		public B withoutReadOnly() {
			this.readOnly = false;
			return getThis();
		}

		/**
		 * Sets the field's range start, meaning only dates later than this value will
		 * be accepted.
		 * 
		 * @param rangeStart
		 *            The start range of the field; may be null
		 * @return this
		 */
		public B withRangeStart(T rangeStart) {
			this.rangeStart = rangeStart;
			return getThis();
		}

		/**
		 * Sets the field's range end, meaning only dates earlier than this value will
		 * be accepted.
		 * 
		 * @param rangeEnd
		 *            The end range of the field; may be null.
		 * @return this
		 */
		public B withRangeEnd(T rangeEnd) {
			this.rangeEnd = rangeEnd;
			return getThis();
		}

		/**
		 * Sets the field's resolution in which the date will be displayed.
		 * 
		 * @param resolution
		 *            The resolution in which to display the date; may <b>not</b> be
		 *            null, DAY by default.
		 * @return this
		 */
		public B withResolution(R resolution) {
			if (resolution == null) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Unable to create a date field with a null resolution.");
			}
			this.resolution = resolution;
			return getThis();
		}

		/**
		 * Sets the field's {@link ZoneId}, which is used when z is included inside the
		 * {@link #withFormat(String)}.
		 * 
		 * @param zoneId
		 *            The zone of the field; may be null.
		 * @return this
		 */
		public B withZoneId(ZoneId zoneId) {
			this.zoneId = zoneId;
			return getThis();
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		@Override
		public PresetPattern<AbstractDateField<T, R>> build() {

			return new PresetPattern<AbstractDateField<T, R>>() {

				private final String dateFormat = TemporalFieldPresetBuilder.this.dateFormat;
				private final String dateOutOfRangeMsgId = TemporalFieldPresetBuilder.this.dateOutOfRangeMessage;
				private final String dateUnparsableMsgId = TemporalFieldPresetBuilder.this.dateUnparsableMessage;
				private final Boolean readOnly = TemporalFieldPresetBuilder.this.readOnly;

				private final T rangeStart = TemporalFieldPresetBuilder.this.rangeStart;
				private final T rangeEnd = TemporalFieldPresetBuilder.this.rangeEnd;
				private final R resolution = TemporalFieldPresetBuilder.this.resolution;
				private final ZoneId zoneId = TemporalFieldPresetBuilder.this.zoneId;

				@Override
				void apply(AbstractDateField<T, R> component) {
					if (this.dateFormat != null) {
						component.setDateFormat(this.dateFormat);
					}
					if (this.dateOutOfRangeMsgId != null) {
						component.setDateOutOfRangeMessage(WebEnv.localize(this.dateOutOfRangeMsgId));
					}
					if (this.dateUnparsableMsgId != null) {
						component.setParseErrorMessage(WebEnv.localize(this.dateUnparsableMsgId));
					}
					if (this.readOnly != null) {
						component.setReadOnly(this.readOnly);
					}
					if (this.rangeStart != null) {
						component.setRangeStart(this.rangeStart);
					}
					if (this.rangeEnd != null) {
						component.setRangeEnd(this.rangeEnd);
					}
					if (this.resolution != null) {
						component.setResolution(this.resolution);
					}
					if (this.zoneId != null) {
						component.setZoneId(this.zoneId);
					}
				}
			};
		}
	}

	/**
	 * {@link PresetBuilder} implementation for {@link PresetPattern}s that can be
	 * applied on {@link AbstractDateField}s using {@link LocalDate}.
	 * <P>
	 * Possible targets for the build {@link PresetPattern} are:<br>
	 * - {@link DateField}<br>
	 * - {@link InlineDateField}
	 */
	public static final class DateFieldPresetBuilder
			extends TemporalFieldPresetBuilder<DateFieldPresetBuilder, LocalDate, DateResolution> {

		private DateFieldPresetBuilder() {
		}

		@Override
		protected DateFieldPresetBuilder getThis() {
			return this;
		}
	}

	/**
	 * {@link PresetBuilder} implementation for {@link PresetPattern}s that can be
	 * applied on {@link AbstractDateField}s using {@link LocalDateTime}.
	 * <P>
	 * Possible targets for the build {@link PresetPattern} are:<br>
	 * - {@link DateTimeField}<br>
	 * - {@link InlineDateTimeField}
	 */
	public static final class DateTimeFieldPresetBuilder
			extends TemporalFieldPresetBuilder<DateTimeFieldPresetBuilder, LocalDateTime, DateTimeResolution> {

		private DateTimeFieldPresetBuilder() {
		}

		protected DateTimeFieldPresetBuilder getThis() {
			return this;
		}
	}

	/**
	 * {@link PresetBuilder} implementation for {@link PresetPattern}s that can be
	 * applied on {@link AbstractTextField}s.
	 * <P>
	 * Possible targets for the build {@link PresetPattern} are:<br>
	 * - {@link TextArea}<br>
	 * - {@link TextField}<br>
	 * - {@link PasswordField}
	 */
	public static final class TextFieldPresetBuilder implements PresetBuilder<AbstractTextField> {

		private Integer maxLength;
		private String placeholder;
		private Boolean readOnly;

		private TextFieldPresetBuilder() {
		}

		/**
		 * Sets the length of the fields value {@link String} to the given maximum
		 * length.
		 * 
		 * @param maxLength
		 *            The max length to set; values &lt;0 are interpreted as unlimited
		 *            length.
		 * @return this
		 */
		public TextFieldPresetBuilder withMaxLength(int maxLength) {
			this.maxLength = maxLength;
			return this;
		}

		/**
		 * Removes the use of a maximum length for the fields value {@link String}.
		 * 
		 * @return this
		 */
		public TextFieldPresetBuilder withoutMaxLength() {
			this.maxLength = -1;
			return this;
		}

		/**
		 * Sets a message to use when the value of the {@link AbstractTextField} is
		 * empty.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param msgId
		 *            The message (or localizable message id) to set; may be null.
		 * @return this
		 */
		public TextFieldPresetBuilder withPlaceholder(String msgId) {
			this.placeholder = msgId;
			return this;
		}

		/**
		 * Removes the use of a message when the value of the {@link AbstractTextField}
		 * is empty.
		 * 
		 * @return this
		 */
		public TextFieldPresetBuilder withoutPlaceholder() {
			this.placeholder = null;
			return this;
		}

		/**
		 * Sets the field to be read only; no changes to the value by the user are
		 * allowed.
		 * 
		 * @return this
		 */
		public TextFieldPresetBuilder withReadOnly() {
			this.readOnly = true;
			return this;
		}

		/**
		 * Sets the field's value to be changeable by the user.
		 * 
		 * @return this
		 */
		public TextFieldPresetBuilder withoutReadOnly() {
			this.readOnly = false;
			return this;
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		@Override
		public PresetPattern<AbstractTextField> build() {
			return new PresetPattern<AbstractTextField>() {

				private final Integer maxLength = TextFieldPresetBuilder.this.maxLength;
				private final String placeholder = TextFieldPresetBuilder.this.placeholder;
				private final Boolean readOnly = TextFieldPresetBuilder.this.readOnly;

				@Override
				void apply(AbstractTextField component) {
					if (this.maxLength != null) {
						component.setMaxLength(this.maxLength);
					}
					if (this.placeholder != null) {
						component.setPlaceholder(WebEnv.localize(this.placeholder));
					}
					if (this.readOnly != null) {
						component.setReadOnly(this.readOnly);
					}
				}
			};
		}
	}

	/**
	 * {@link PresetBuilder} implementation for {@link PresetPattern}s that can be
	 * applied on {@link CheckBox}s.
	 */
	public static final class CheckBoxPresetBuilder implements PresetBuilder<CheckBox> {

		private Boolean readOnly;

		private CheckBoxPresetBuilder() {
		}

		/**
		 * Sets the field to be read only; no changes to the value by the user are
		 * allowed.
		 * 
		 * @return this
		 */
		public CheckBoxPresetBuilder withReadOnly() {
			this.readOnly = true;
			return this;
		}

		/**
		 * Sets the field's value to be changeable by the user.
		 * 
		 * @return this
		 */
		public CheckBoxPresetBuilder withoutReadOnly() {
			this.readOnly = false;
			return this;
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		@Override
		public PresetPattern<CheckBox> build() {
			return new PresetPattern<CheckBox>() {

				private final Boolean readOnly = CheckBoxPresetBuilder.this.readOnly;

				@Override
				void apply(CheckBox component) {
					if (this.readOnly != null) {
						component.setReadOnly(this.readOnly);
					}
				}
			};
		}
	}

	/**
	 * {@link PresetBuilder} implementation for {@link PresetPattern}s that can be
	 * applied on {@link CheckBoxGroup}s.
	 */
	public static final class CheckBoxGroupPresetBuilder implements PresetBuilder<CheckBoxGroup<?>> {

		private Boolean htmlContentAllowed;
		private Boolean readOnly;

		private CheckBoxGroupPresetBuilder() {
		}

		/**
		 * Instructs the {@link CheckBoxGroup} to interpret the item captions as HTML
		 * code.
		 * 
		 * @return this
		 */
		public CheckBoxGroupPresetBuilder withHtmlContent() {
			this.htmlContentAllowed = true;
			return this;
		}

		/**
		 * Instructs the {@link CheckBoxGroup} to not interpret the item captions as
		 * HTML code.
		 * 
		 * @return this
		 */
		public CheckBoxGroupPresetBuilder withoutHtmlContent() {
			this.htmlContentAllowed = false;
			return this;
		}

		/**
		 * Sets the field to be read only; no changes to the value by the user are
		 * allowed.
		 * 
		 * @return this
		 */
		public CheckBoxGroupPresetBuilder withReadOnly() {
			this.readOnly = true;
			return this;
		}

		/**
		 * Sets the field's value to be changeable by the user.
		 * 
		 * @return this
		 */
		public CheckBoxGroupPresetBuilder withoutReadOnly() {
			this.readOnly = false;
			return this;
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		@Override
		public PresetPattern<CheckBoxGroup<?>> build() {
			return new PresetPattern<CheckBoxGroup<?>>() {

				private final Boolean htmlContentAllowed = CheckBoxGroupPresetBuilder.this.htmlContentAllowed;
				private final Boolean readOnly = CheckBoxGroupPresetBuilder.this.readOnly;

				@Override
				void apply(CheckBoxGroup<?> component) {
					if (this.htmlContentAllowed != null) {
						component.setHtmlContentAllowed(this.htmlContentAllowed);
					}
					if (this.readOnly != null) {
						component.setReadOnly(this.readOnly);
					}
				}
			};
		}
	}

	/**
	 * {@link PresetBuilder} implementation for {@link PresetPattern}s that can be
	 * applied on {@link ComboBox}es.
	 */
	public static final class ComboBoxPresetBuilder implements PresetBuilder<ComboBox<?>> {

		private Boolean emptySelectionAllowed;
		private String emptySelectionCaptionMsgId = StringUtils.EMPTY;
		private Integer pageLength;
		private String placeholder;
		private Boolean textInputAllowed;
		private Boolean readOnly;

		private ComboBoxPresetBuilder() {
		}

		/**
		 * Allows the user to manually select an artificial empty value to repeal any
		 * concrete item selection. Sets the empty value caption to an empty
		 * {@link String}.
		 * 
		 * @return this
		 */
		public ComboBoxPresetBuilder withEmptySelection() {
			this.emptySelectionAllowed = true;
			this.emptySelectionCaptionMsgId = StringUtils.EMPTY;
			return this;
		}

		/**
		 * Allows the user to manually select an artificial empty value to repeal any
		 * concrete item selection. Sets the empty value caption to the given value.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param emptySelectionCaptionMsgId
		 *            The caption (or localizable message id) of the artificial empty
		 *            value to set; may be null.
		 * @return this
		 */
		public ComboBoxPresetBuilder withEmptySelection(String emptySelectionCaptionMsgId) {
			this.emptySelectionAllowed = true;
			this.emptySelectionCaptionMsgId = StringUtils.defaultIfBlank(emptySelectionCaptionMsgId, StringUtils.EMPTY);
			return this;
		}

		/**
		 * Permits the user to manually select an artificial empty value to repeal any
		 * concrete item selection.
		 * 
		 * @return this
		 */
		public ComboBoxPresetBuilder withoutEmptySelection() {
			this.emptySelectionAllowed = false;
			this.emptySelectionCaptionMsgId = StringUtils.EMPTY;
			return this;
		}

		/**
		 * Sets the length of the suggestion popup list.
		 * 
		 * @param pageLength
		 *            The length of the suggestion popup list; 0 will be interpreted to
		 *            disable suggestion popup, all items are visible.
		 * @return this
		 */
		public ComboBoxPresetBuilder withPageLength(int pageLength) {
			this.pageLength = pageLength;
			return this;
		}

		/**
		 * Sets a message to use when the value of the {@link ComboBox} is empty.
		 * <P>
		 * Use of a message id is allowed here to use auto-localization via
		 * {@link WebEnv}.
		 * 
		 * @param msgId
		 *            The message (or localizable message id) to set; may be null.
		 * @return this
		 */
		public ComboBoxPresetBuilder withPlaceholder(String msgId) {
			this.placeholder = msgId;
			return this;
		}

		/**
		 * Removes the use of a message when the value of the {@link ComboBox} is empty.
		 * 
		 * @return this
		 */
		public ComboBoxPresetBuilder withoutPlaceholder() {
			this.placeholder = null;
			return this;
		}

		/**
		 * Allows the user to directly type into the {@link ComboBox} field.
		 * 
		 * @return this
		 */
		public ComboBoxPresetBuilder withTextInput() {
			this.textInputAllowed = true;
			return this;
		}

		/**
		 * Permits the user to directly type into the {@link ComboBox} field.
		 * 
		 * @return this
		 */
		public ComboBoxPresetBuilder withoutTextInput() {
			this.textInputAllowed = false;
			return this;
		}

		/**
		 * Sets the field to be read only; no changes to the value by the user are
		 * allowed.
		 * 
		 * @return this
		 */
		public ComboBoxPresetBuilder withReadOnly() {
			this.readOnly = true;
			return this;
		}

		/**
		 * Sets the field's value to be changeable by the user.
		 * 
		 * @return this
		 */
		public ComboBoxPresetBuilder withoutReadOnly() {
			this.readOnly = false;
			return this;
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		@Override
		public PresetPattern<ComboBox<?>> build() {
			return new PresetPattern<ComboBox<?>>() {

				private final Boolean emptySelectionAllowed = ComboBoxPresetBuilder.this.emptySelectionAllowed;
				private final String emptySelectionCaptionMsgId = ComboBoxPresetBuilder.this.emptySelectionCaptionMsgId;
				private final Integer pageLength = ComboBoxPresetBuilder.this.pageLength;
				private final String placeholder = ComboBoxPresetBuilder.this.placeholder;
				private final Boolean textInputAllowed = ComboBoxPresetBuilder.this.textInputAllowed;
				private final Boolean readOnly = ComboBoxPresetBuilder.this.readOnly;

				@Override
				public void apply(ComboBox<?> component) {
					if (this.emptySelectionAllowed != null) {
						component.setEmptySelectionAllowed(this.emptySelectionAllowed);
						component.setEmptySelectionCaption(WebEnv.localize(this.emptySelectionCaptionMsgId));
					}
					if (this.pageLength != null) {
						component.setPageLength(this.pageLength);
					}
					if (this.placeholder != null) {
						component.setPlaceholder(WebEnv.localize(this.placeholder));
					}
					if (this.textInputAllowed != null) {
						component.setTextInputAllowed(this.textInputAllowed);
					}
					if (this.readOnly != null) {
						component.setReadOnly(this.readOnly);
					}
				}
			};
		}
	}

	/**
	 * {@link PresetBuilder} implementation for {@link PresetPattern}s that can be
	 * applied on {@link RadioButtonGroup}s.
	 */
	public static final class RadioButtonGroupPresetBuilder implements PresetBuilder<RadioButtonGroup<?>> {

		private Boolean htmlContentAllowed;
		private Boolean readOnly;

		private RadioButtonGroupPresetBuilder() {
		}

		/**
		 * Instructs the {@link RadioButtonGroup} to interpret the item captions as HTML
		 * code.
		 * 
		 * @return this
		 */
		public RadioButtonGroupPresetBuilder withHtmlContent() {
			this.htmlContentAllowed = true;
			return this;
		}

		/**
		 * Instructs the {@link RadioButtonGroup} to not interpret the item captions as
		 * HTML code.
		 * 
		 * @return this
		 */
		public RadioButtonGroupPresetBuilder withoutHtmlContent() {
			this.htmlContentAllowed = false;
			return this;
		}

		/**
		 * Sets the field to be read only; no changes to the value by the user are
		 * allowed.
		 * 
		 * @return this
		 */
		public RadioButtonGroupPresetBuilder withReadOnly() {
			this.readOnly = true;
			return this;
		}

		/**
		 * Sets the field's value to be changeable by the user.
		 * 
		 * @return this
		 */
		public RadioButtonGroupPresetBuilder withoutReadOnly() {
			this.readOnly = false;
			return this;
		}

		/**
		 * Builds a new pattern on every invocation.
		 * 
		 * @return A new {@link PresetPattern} with the values currently set; never null
		 */
		@Override
		public PresetPattern<RadioButtonGroup<?>> build() {
			return new PresetPattern<RadioButtonGroup<?>>() {

				private final Boolean htmlContentAllowed = RadioButtonGroupPresetBuilder.this.htmlContentAllowed;
				private final Boolean readOnly = RadioButtonGroupPresetBuilder.this.readOnly;

				@Override
				void apply(RadioButtonGroup<?> component) {
					if (this.htmlContentAllowed != null) {
						component.setHtmlContentAllowed(this.htmlContentAllowed);
					}
					if (this.readOnly != null) {
						component.setReadOnly(this.readOnly);
					}
				}
			};
		}
	}

	// ##############################################################################################################
	// ################################################## INIT ######################################################
	// ##############################################################################################################

	/**
	 * Begins a building process for {@link PresetPattern}s for
	 * {@link AbstractOrderedLayout}s.
	 * 
	 * @return A new {@link OrderedLayoutPresetBuilder}; never null
	 */
	public static final OrderedLayoutPresetBuilder forOrderedLayout() {
		return new OrderedLayoutPresetBuilder();
	}

	/**
	 * Begins a building process for {@link PresetPattern}s for
	 * {@link AbstractDateField}s using {@link LocalDate}.
	 * 
	 * @return A new {@link DateFieldPresetBuilder}; never null
	 */
	public static final DateFieldPresetBuilder forDateField() {
		return new DateFieldPresetBuilder();
	}

	/**
	 * Begins a building process for {@link PresetPattern}s for
	 * {@link AbstractDateField}s using {@link LocalDateTime}.
	 * 
	 * @return A new {@link DateTimeFieldPresetBuilder}; never null
	 */
	public static final DateTimeFieldPresetBuilder forDateTimeField() {
		return new DateTimeFieldPresetBuilder();
	}

	/**
	 * Begins a building process for {@link PresetPattern}s for
	 * {@link AbstractTextField}s.
	 * 
	 * @return A new {@link TextFieldPresetBuilder}; never null
	 */
	public static final TextFieldPresetBuilder forTextField() {
		return new TextFieldPresetBuilder();
	}

	/**
	 * Begins a building process for {@link PresetPattern}s for {@link CheckBox}s.
	 * 
	 * @return A new {@link CheckBoxPresetBuilder}; never null
	 */
	public static final CheckBoxPresetBuilder forCheckBox() {
		return new CheckBoxPresetBuilder();
	}

	/**
	 * Begins a building process for {@link PresetPattern}s for
	 * {@link CheckBoxGroup}s.
	 * 
	 * @return A new {@link CheckBoxGroupPresetBuilder}; never null
	 */
	public static final CheckBoxGroupPresetBuilder forCheckBoxGroup() {
		return new CheckBoxGroupPresetBuilder();
	}

	/**
	 * Begins a building process for {@link PresetPattern}s for {@link ComboBox}es.
	 * 
	 * @return A new {@link ComboBoxPresetBuilder}; never null
	 */
	public static final ComboBoxPresetBuilder forComboBox() {
		return new ComboBoxPresetBuilder();
	}

	/**
	 * Begins a building process for {@link PresetPattern}s for
	 * {@link RadioButtonGroup}s.
	 * 
	 * @return A new {@link RadioButtonGroupPresetBuilder}; never null
	 */
	public static final RadioButtonGroupPresetBuilder forRadioButtonGroup() {
		return new RadioButtonGroupPresetBuilder();
	}
}
