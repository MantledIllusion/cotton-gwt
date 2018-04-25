package com.mantledillusion.vaadin.cotton.viewpresenter;

import java.lang.reflect.Constructor;

import com.mantledillusion.injection.hura.Processor.Phase;
import com.mantledillusion.injection.hura.annotation.Process;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Basic super type for {@link Presented} {@link View}s that are embedded into
 * their own {@link Window}.
 * <p>
 * Note that since the {@link Frame} will be embedded into a {@link Window}, it
 * cannot be added as a child to a different parent {@link Component}; doing so
 * will result in a {@link WebException}.
 */
public abstract class Frame extends View {

	private static final long serialVersionUID = 1L;

	public static final String FRAME_COMPONENT_ID = "_frame";

	private Window window = new Window();

	/**
	 * Base {@link Constructor}.
	 * <p>
	 * Sets the following settings to defaults:<br>
	 * {@link #setClosable(boolean)} -&gt; true<br>
	 * {@link #setModal(boolean)} -&gt; false<br>
	 * {@link #setDraggable(boolean)} -&gt; true<br>
	 * {@link #setResizable(boolean)} -&gt; true<br>
	 */
	protected Frame() {
		this(true, false, true, true);
	}

	/**
	 * Advanced {@link Constructor}.
	 * <p>
	 * Sets the given settings to the given values.
	 * <p>
	 * Sets the following settings to defaults:<br>
	 * {@link #setDraggable(boolean)} -&gt; true<br>
	 * {@link #setResizable(boolean)} -&gt; true<br>
	 * 
	 * @param closable
	 *            {@link #setClosable(boolean)}
	 * @param modal
	 *            {@link #setModal(boolean)}
	 */
	protected Frame(boolean closable, boolean modal) {
		this(closable, modal, true, true);
	}

	/**
	 * Advanced {@link Constructor}.
	 * <p>
	 * Sets the given settings to the given values.
	 * 
	 * @param closable
	 *            {@link #setClosable(boolean)}
	 * @param modal
	 *            {@link #setModal(boolean)}
	 * @param draggable
	 *            {@link #setDraggable(boolean)}
	 * @param resizable
	 *            {@link #setResizable(boolean)}
	 */
	protected Frame(boolean closable, boolean modal, boolean draggable, boolean resizable) {
		this.window.setClosable(closable);
		this.window.setModal(modal);
		this.window.setDraggable(draggable);
		this.window.setResizable(resizable);
	}

	@Override
	Component setupUI(TemporalActiveComponentRegistry reg) throws Throwable {
		this.allowSetParent = true;
		this.window.setContent(this);
		this.allowSetParent = false;

		Component ui = super.setupUI(reg);
		reg.registerActiveComponent(FRAME_COMPONENT_ID, this.window);

		return ui;
	}

	private boolean allowSetParent = false;

	/**
	 * Framework internal method <b>(DO NOT USE!)</b>
	 * <p>
	 * A {@link Frame}'s parent can only be the {@link Window} it is attached to.
	 */
	@Override
	public final void setParent(HasComponents parent) {
		if (this.allowSetParent) {
			super.setParent(parent);
		} else {
			throw new WebException(HttpErrorCodes.HTTP903_NOT_IMPLEMENTED_ERROR, Frame.class.getSimpleName()
					+ " types cannot be added to any other component than their own window.");
		}
	}
	
	@Process(Phase.DESTROY)
	private void detachWindowFromUI() {
		this.window.close();
	}

	// #########################################################################################################################################
	// ########################################################## WINDOW SHOW/CLOSE ############################################################
	// #########################################################################################################################################

	/**
	 * Adds this frame to the current {@link UI} to show it.
	 */
	protected final void show() {
		UI.getCurrent().addWindow(this.window);
	}

	/**
	 * Closes this frame.
	 */
	protected final void close() {
		this.window.close();
	}

	// #########################################################################################################################################
	// ###################################################### DELEGATION WINDOW HANDLING #######################################################
	// #########################################################################################################################################

	/**
	 * {@link Window#isClosable()}
	 * 
	 * @return true if the window can be closed by the user.
	 */
	protected final boolean isClosable() {
		return this.window.isClosable();
	}

	/**
	 * {@link Window#setClosable(boolean)}
	 * 
	 * @param closable
	 *            determines if the window can be closed by the user.
	 */
	protected final void setClosable(boolean closable) {
		this.window.setClosable(closable);
	}

	/**
	 * {@link Window#addCloseShortcut(int, int...)}
	 * 
	 * @param keyCode
	 *            the keycode for invoking the shortcut
	 * @param modifiers
	 *            the (optional) modifiers for invoking the shortcut. Can be set to
	 *            null to be explicit about not having modifiers.
	 */
	protected final void addCloseShortcut(int keyCode, int... modifiers) {
		this.window.addCloseShortcut(keyCode, modifiers);
	}

	/**
	 * {@link Window#isDraggable()}
	 * 
	 * @return true if window is draggable; false if not
	 */
	protected final boolean isDraggable() {
		return this.window.isDraggable();
	}

	/**
	 * {@link Window#setDraggable(boolean)}
	 * 
	 * @param draggable
	 *            true if the window can be dragged by the user
	 */
	protected final void setDraggable(boolean draggable) {
		this.window.setDraggable(draggable);
	}

	/**
	 * {@link Window#isModal()}
	 * 
	 * @return true if this window is modal.
	 */
	protected final boolean isModal() {
		return this.window.isModal();
	}

	/**
	 * {@link Window#setModal(boolean)}
	 * 
	 * @param modal
	 *            true if modality is to be turned on
	 */
	protected final void setModal(boolean modal) {
		this.window.setModal(modal);
	}

	/**
	 * {@link Window#isResizable()}
	 * 
	 * @return true if window is resizable by the end-user, otherwise false.
	 */
	protected final boolean isResizable() {
		return this.window.isResizable();
	}

	/**
	 * {@link Window#setResizable(boolean)}
	 * 
	 * @param resizable
	 *            true if resizability is to be turned on
	 */
	protected final void setResizable(boolean resizable) {
		this.window.setResizable(resizable);
	}

	// #########################################################################################################################################
	// #################################################### DELEGATION WINDOW POSITIONING ######################################################
	// #########################################################################################################################################

	/**
	 * {@link Window#getPositionX()}
	 * 
	 * @return the Distance of Window left border in pixels from left border of the
	 *         containing (main window).or -1 if unspecified
	 */
	protected int getPositionX() {
		return this.window.getPositionX();
	}

	/**
	 * {@link Window#setPositionX(int)}
	 * 
	 * @param x
	 *            the Distance of Window left border in pixels from left border of
	 *            the containing (main window). or -1 if unspecified.
	 */
	protected void setPositionX(int x) {
		this.window.setPositionX(x);
	}

	/**
	 * {@link Window#getPositionY()}
	 * 
	 * @return Distance of Window top border in pixels from top border of the
	 *         containing (main window). or -1 if unspecified
	 */
	protected int getPositionY() {
		return this.window.getPositionY();
	}

	/**
	 * {@link Window#setPositionY(int)}
	 * 
	 * @param y
	 *            the Distance of Window top border in pixels from top border of the
	 *            containing (main window). or -1 if unspecified
	 */
	protected void setPositionY(int y) {
		this.window.setPositionY(y);
	}

	/**
	 * {@link Window#setPosition(int, int)}
	 * 
	 * @param x
	 *            The new x coordinate for the window
	 * @param y
	 *            The new y coordinate for the window
	 */
	protected void setPosition(int x, int y) {
		this.window.setPosition(x, y);
	}

	/**
	 * {@link Window#center()}
	 */
	protected void center() {
		this.window.center();
	}

	// #########################################################################################################################################
	// ###################################################### OVERWRITTEN FOR DELEGATION #######################################################
	// #########################################################################################################################################

	@Override
	public String getCaption() {
		return this.window.getCaption();
	}

	@Override
	public void setCaption(String caption) {
		this.window.setCaption(caption);
	}

	@Override
	public boolean isCaptionAsHtml() {
		return this.window.isCaptionAsHtml();
	}

	@Override
	public void setCaptionAsHtml(boolean captionAsHtml) {
		this.window.setCaptionAsHtml(captionAsHtml);
	}

	@Override
	public String getDescription() {
		return this.window.getDescription();
	}

	@Override
	public void setDescription(String description) {
		this.window.setDescription(description);
	}

	@Override
	public void setDescription(String description, ContentMode mode) {
		this.window.setDescription(description, mode);
	}

	@Override
	public Resource getIcon() {
		return this.window.getIcon();
	}

	@Override
	public void setIcon(Resource icon) {
		this.window.setIcon(icon);
	}

	@Override
	public String getId() {
		return this.window.getId();
	}

	@Override
	public void setId(String id) {
		this.window.setId(id);
	}

	@Override
	public String getStyleName() {
		return this.window.getStyleName();
	}

	@Override
	public void setStyleName(String style) {
		this.window.setStyleName(style);
	}

	@Override
	public void setStyleName(String style, boolean add) {
		this.window.setStyleName(style, add);
	}

	@Override
	public void addStyleName(String style) {
		this.window.addStyleName(style);
	}

	@Override
	public void addStyleNames(String... styles) {
		this.window.addStyleNames(styles);
	}

	@Override
	public void removeStyleName(String style) {
		this.window.removeStyleName(style);
	}

	@Override
	public void removeStyleNames(String... styles) {
		this.window.removeStyleNames(styles);
	}

	@Override
	public final void setWidth(String width) {
		this.window.setWidth(width);
	}

	@Override
	public final void setWidth(float width, Unit unit) {
		this.window.setWidth(width, unit);
	}

	@Override
	public final void setWidthUndefined() {
		this.window.setWidthUndefined();
	}

	@Override
	public final void setHeight(String height) {
		this.window.setHeight(height);
	}

	@Override
	public final void setHeight(float height, Unit unit) {
		this.window.setHeight(height, unit);
	}

	@Override
	public final void setHeightUndefined() {
		this.window.setHeightUndefined();
	}

	@Override
	public boolean isEnabled() {
		return this.window.isEnabled();
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.window.setEnabled(enabled);
	}

	@Override
	public boolean isVisible() {
		return this.window.isVisible();
	}

	@Override
	public void setVisible(boolean visible) {
		this.window.setVisible(visible);
	}
}
