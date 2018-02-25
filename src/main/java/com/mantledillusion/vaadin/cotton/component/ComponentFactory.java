package com.mantledillusion.vaadin.cotton.component;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;

import com.mantledillusion.vaadin.cotton.NavigationTarget;
import com.mantledillusion.vaadin.cotton.WebEnv;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Panel;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Static factory for building implementations of {@link AbstractComponent}s.
 */
public final class ComponentFactory {

	/**
	 * Base type for option patterns.
	 * <P>
	 * Option patterns are sets of configuration options that can be applied on
	 * instances of a specific {@link AbstractComponent} implementation.
	 * <P>
	 * For all normal cases, these default {@link OptionPattern} implementations
	 * should be sufficient:<BR>
	 * - {@link NamingPattern} for captions, descriptions, etc.<BR>
	 * - {@link SizingPattern} for width and height<BR>
	 * - {@link StylingPattern} for icon and CSS<BR>
	 * - {@link PresetPattern} for {@link AbstractComponent} sub type specific
	 * options.
	 * 
	 * @param <T>
	 *            The {@link AbstractComponent} implementing type this option
	 *            pattern can be applied on.
	 */
	public static abstract class OptionPattern<T extends AbstractComponent> {

		/**
		 * Applies all of this pattern's options to the given {@link AbstractComponent}.
		 * 
		 * @param component
		 *            The {@link AbstractComponent} instance to apply this option
		 *            pattern on; may <B>NOT<B> be null.
		 */
		abstract void apply(T component);

		/**
		 * Groups the given {@link OptionPattern}s together.
		 * <p>
		 * May be used to create a commutative pattern for a specific type of
		 * {@link Component} using multiple {@link NamingPattern}s,
		 * {@link SizingPattern}s, etc grouped together.
		 * 
		 * @param <T>
		 *            The {@link AbstractComponent} extending type whose
		 *            {@link OptionPattern}s to group.
		 * @param patterns
		 *            The pattern to group; may be null or contain nulls, both is
		 *            ignored.
		 * @return A new {@link OptionPattern} that has no own options that it applies
		 *         to a component, but applies the given patterns to it; never null
		 */
		@SafeVarargs
		public static <T extends AbstractComponent> OptionPattern<T> of(OptionPattern<? super T>... patterns) {
			return new OptionPattern<T>() {

				@Override
				void apply(T component) {
					Arrays.asList(patterns).stream().forEach(pattern -> {
						if (pattern != null) {
							pattern.apply(component);
						}
					});
				}
			};
		}
	}

	// ##############################################################################################################
	// ################################################### INIT #####################################################
	// ##############################################################################################################

	private ComponentFactory() {
	}

	/**
	 * Base {@link Method} that is used in all of {@link ComponentFactory}'s factory
	 * methods to apply {@link OptionPattern}s on an {@link AbstractComponent}.
	 * 
	 * @param <T>
	 *            The type of {@link AbstractComponent} to apply the given patterns
	 *            on.
	 * @param component
	 *            The {@link AbstractComponent} to apply {@link OptionPattern}s on;
	 *            may be null, then the patterns will not be called.
	 * @param patterns
	 *            The patterns to apply on the given {@link AbstractComponent}; may
	 *            be null or empty, then nothing will be applied. Will be applied in
	 *            the given order.
	 * @return The given {@link AbstractComponent}, for in-line use.
	 */
	@SafeVarargs
	public static <T extends AbstractComponent> T apply(T component, OptionPattern<? super T>... patterns) {
		if (component != null && patterns != null) {
			for (OptionPattern<? super T> pattern : patterns) {
				if (pattern != null) {
					pattern.apply(component);
				}
			}
		}
		return component;
	}

	// ##############################################################################################################
	// ################################################# LAYOUT #####################################################
	// ##############################################################################################################

	/**
	 * Factory method for an {@link AbsoluteLayout}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link AbsoluteLayout}
	 *            before returning; may be null or empty, then nothing will be
	 *            applied. Will be applied in the given order.
	 * @return A new {@link AbsoluteLayout} instance; never null
	 */
	@SafeVarargs
	public static AbsoluteLayout buildAbsoluteLayout(OptionPattern<? super AbsoluteLayout>... patterns) {
		return apply(new AbsoluteLayout(), patterns);
	}

	/**
	 * Factory method for a {@link HorizontalLayout}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link HorizontalLayout}
	 *            before returning; may be null or empty, then nothing will be
	 *            applied. Will be applied in the given order.
	 * @return A new {@link HorizontalLayout} instance; never null
	 */
	@SafeVarargs
	public static HorizontalLayout buildHorizontalLayout(OptionPattern<? super HorizontalLayout>... patterns) {
		return apply(new HorizontalLayout(), patterns);
	}

	/**
	 * Factory method for a {@link VerticalLayout}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link VerticalLayout}
	 *            before returning; may be null or empty, then nothing will be
	 *            applied. Will be applied in the given order.
	 * @return A new {@link VerticalLayout} instance; never null
	 */
	@SafeVarargs
	public static VerticalLayout buildVerticalLayout(OptionPattern<? super VerticalLayout>... patterns) {
		return apply(new VerticalLayout(), patterns);
	}

	// ##############################################################################################################
	// ################################################## PANEL #####################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link Panel}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link Panel} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link Panel} instance; never null
	 */
	@SafeVarargs
	public static Panel buildPanel(OptionPattern<? super Panel>... patterns) {
		return apply(new Panel(), patterns);
	}

	// ##############################################################################################################
	// ################################################### IMAGE ####################################################
	// ##############################################################################################################

	/**
	 * Factory method for an {@link Image}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link Image} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link Image} instance; never null
	 */
	@SafeVarargs
	public static Image buildImage(OptionPattern<? super Image>... patterns) {
		return apply(new Image(), patterns);
	}

	/**
	 * Factory method for an {@link Image}.
	 * 
	 * @param imageResource
	 *            The {@link Resource} to set to the requested {@link Image}.
	 * @param patterns
	 *            The patterns to apply on the requested {@link Image} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link Image} instance; never null
	 */
	@SafeVarargs
	public static Image buildImage(Resource imageResource, OptionPattern<? super Image>... patterns) {
		return apply(new Image(null, imageResource), patterns);
	}

	// ##############################################################################################################
	// ################################################## LINK ######################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link Link}.
	 * 
	 * @param target
	 *            The {@link NavigationTarget} to build a {@link Link} to;
	 *            <b>not</b> allowed to be null.
	 * @param patterns
	 *            The patterns to apply on the requested {@link Image} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link Link} instance; never null
	 */
	@SafeVarargs
	public static Link buildLink(NavigationTarget target, OptionPattern<? super Link>... patterns) {
		if (target == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Unable to create a link to a null target.");
		}
		URI uri = Page.getCurrent().getLocation();
		String url = uri.getScheme() + "://" + uri.getAuthority() + target.toUrl();
		return buildLink(url, patterns);
	}

	/**
	 * Factory method for a {@link Link}.
	 * 
	 * @param url
	 *            The URL the requested {@link Link} addresses.
	 * @param patterns
	 *            The patterns to apply on the requested {@link Image} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link Link} instance; never null
	 */
	@SafeVarargs
	public static Link buildLink(String url, OptionPattern<? super Link>... patterns) {
		return apply(new Link(null, new ExternalResource(url)), patterns);
	}

	// ##############################################################################################################
	// ################################################## LABEL #####################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link Label}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link Label} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link Label} instance; never null
	 */
	@SafeVarargs
	public static Label buildLabel(OptionPattern<? super Label>... patterns) {
		return buildLabel(null, patterns);
	}

	/**
	 * Factory method for a {@link Label}.
	 * <P>
	 * Use of a message id is allowed here to use auto-localization via
	 * {@link WebEnv}.
	 * 
	 * @param textMsgId
	 *            The text (or localizable text message id) the {@link Label} has to
	 *            have; may be null.
	 * @param patterns
	 *            The patterns to apply on the requested {@link Label} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link Label} instance; never null
	 */
	@SafeVarargs
	public static Label buildLabel(String textMsgId, OptionPattern<? super Label>... patterns) {
		return apply(new Label(WebEnv.localize(textMsgId)), patterns);
	}

	// ##############################################################################################################
	// ################################################ TEXTFIELD ###################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link TextField}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link TextField} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link TextField} instance; never null
	 */
	@SafeVarargs
	public static TextField buildTextField(OptionPattern<? super TextField>... patterns) {
		return apply(new TextField(), patterns);
	}

	// ##############################################################################################################
	// ################################################# TEXTAREA ###################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link TextArea}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link TextArea} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link TextArea} instance; never null
	 */
	@SafeVarargs
	public static TextArea buildTextArea(OptionPattern<? super TextArea>... patterns) {
		return apply(new TextArea(), patterns);
	}

	// ##############################################################################################################
	// ################################################## BUTTON ####################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link Button}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link Button} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link Button} instance; never null
	 */
	@SafeVarargs
	public static Button buildButton(OptionPattern<? super Button>... patterns) {
		return apply(new Button(), patterns);
	}

	// ##############################################################################################################
	// ################################################# MENU BAR ###################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link MenuBar}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link MenuBar} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link MenuBar} instance; never null
	 */
	@SafeVarargs
	public static MenuBar buildMenuBar(OptionPattern<? super MenuBar>... patterns) {
		return apply(new MenuBar(), patterns);
	}

	// ##############################################################################################################
	// ################################################# CHECKBOX ###################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link CheckBox}.
	 * 
	 * @param patterns
	 *            The patterns to apply on the requested {@link CheckBox} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link CheckBox} instance; never null
	 */
	@SafeVarargs
	public static CheckBox buildCheckBox(OptionPattern<? super CheckBox>... patterns) {
		return apply(new CheckBox(), patterns);
	}

	// ##############################################################################################################
	// ############################################## CHECKBOX GROUP ################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link CheckBoxGroup}.
	 * 
	 * @param <T>
	 *            The element type of the {@link CheckBoxGroup}.
	 * @param patterns
	 *            The patterns to apply on the requested {@link CheckBoxGroup}
	 *            before returning; may be null or empty, then nothing will be
	 *            applied. Will be applied in the given order.
	 * @return A new {@link CheckBoxGroup} instance; never null
	 */
	@SafeVarargs
	public static <T> CheckBoxGroup<T> buildCheckBoxGroup(OptionPattern<? super CheckBoxGroup<T>>... patterns) {
		return apply(new CheckBoxGroup<T>(), patterns);
	}

	// ##############################################################################################################
	// ############################################ RADIOBUTTON GROUP ###############################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link RadioButtonGroup}.
	 * 
	 * @param <T>
	 *            The element type of the {@link RadioButtonGroup}.
	 * @param patterns
	 *            The patterns to apply on the requested {@link RadioButtonGroup}
	 *            before returning; may be null or empty, then nothing will be
	 *            applied. Will be applied in the given order.
	 * @return A new {@link RadioButtonGroup} instance; never null
	 */
	@SafeVarargs
	public static <T> RadioButtonGroup<T> buildRadioButtonGroup(
			OptionPattern<? super RadioButtonGroup<T>>... patterns) {
		return apply(new RadioButtonGroup<T>(), patterns);
	}

	// ##############################################################################################################
	// ################################################# COMBOBOX ###################################################
	// ##############################################################################################################

	/**
	 * Factory method for a {@link ComboBox}.
	 * 
	 * @param <T>
	 *            The element type of the {@link ComboBox}.
	 * @param patterns
	 *            The patterns to apply on the requested {@link ComboBox} before
	 *            returning; may be null or empty, then nothing will be applied.
	 *            Will be applied in the given order.
	 * @return A new {@link ComboBox} instance; never null
	 */
	@SafeVarargs
	public static <T> ComboBox<T> buildComboBox(OptionPattern<? super ComboBox<T>>... patterns) {
		return apply(new ComboBox<T>(), patterns);
	}
}
