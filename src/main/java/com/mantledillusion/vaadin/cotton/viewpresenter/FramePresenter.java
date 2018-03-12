package com.mantledillusion.vaadin.cotton.viewpresenter;

import java.lang.reflect.Method;

import com.mantledillusion.vaadin.cotton.CottonUI;
import com.vaadin.ui.Window;

/**
 * Basic super type for a {@link Presenter} that controls a {@link Frame}.
 * <p>
 * This extension to {@link Presenter} for {@link Frame} {@link View}s has
 * access to the {@link Window} specific protected {@link Method}s of
 * {@link Frame}, like {@link Frame#show()} or
 * {@link Frame#setPosition(int, int)}.
 *
 * @param <T>
 *            The type of {@link Frame} this {@link FramePresenter} can control.
 */
public abstract class FramePresenter<T extends Frame> extends Presenter<T> {

	// #########################################################################################################################################
	// ########################################################## WINDOW SHOW/CLOSE ############################################################
	// #########################################################################################################################################

	/**
	 * Adds the frame to the current {@link CottonUI} to show it.
	 */
	protected final void showFrame() {
		getView().show();
	}

	/**
	 * Closes the frame.
	 */
	protected final void closeFrame() {
		getView().close();
	}

	// #########################################################################################################################################
	// ###################################################### DELEGATION WINDOW HANDLING #######################################################
	// #########################################################################################################################################

	/**
	 * {@link Frame#isClosable()}
	 * 
	 * @return true if the window can be closed by the user.
	 */
	protected final boolean isFrameClosable() {
		return getView().isClosable();
	}

	/**
	 * {@link Frame#setClosable(boolean)}
	 * 
	 * @param closable
	 *            determines if the window can be closed by the user.
	 */
	protected final void setFrameClosable(boolean closable) {
		getView().setClosable(closable);
	}

	/**
	 * {@link Frame#addCloseShortcut(int, int...)}
	 * 
	 * @param keyCode
	 *            the keycode for invoking the shortcut
	 * @param modifiers
	 *            the (optional) modifiers for invoking the shortcut. Can be set to
	 *            null to be explicit about not having modifiers.
	 */
	protected final void addFrameCloseShortcut(int keyCode, int... modifiers) {
		getView().addCloseShortcut(keyCode, modifiers);
	}

	/**
	 * {@link Frame#isDraggable()}
	 * 
	 * @return true if window is draggable; false if not
	 */
	protected final boolean isFrameDraggable() {
		return getView().isDraggable();
	}

	/**
	 * {@link Frame#setDraggable(boolean)}
	 * 
	 * @param draggable
	 *            true if the window can be dragged by the user
	 */
	protected final void setFrameDraggable(boolean draggable) {
		getView().setDraggable(draggable);
	}

	/**
	 * {@link Frame#isModal()}
	 * 
	 * @return true if this window is modal.
	 */
	protected final boolean isFrameModal() {
		return getView().isModal();
	}

	/**
	 * {@link Frame#setModal(boolean)}
	 * 
	 * @param modal
	 *            true if modality is to be turned on
	 */
	protected final void setFrameModal(boolean modal) {
		getView().setModal(modal);
	}

	/**
	 * {@link Frame#isResizable()}
	 * 
	 * @return true if window is resizable by the end-user, otherwise false.
	 */
	protected final boolean isFrameResizable() {
		return getView().isResizable();
	}

	/**
	 * {@link Frame#setResizable(boolean)}
	 * 
	 * @param resizable
	 *            true if resizability is to be turned on
	 */
	protected final void setFrameResizable(boolean resizable) {
		getView().setResizable(resizable);
	}

	// #########################################################################################################################################
	// #################################################### DELEGATION WINDOW POSITIONING ######################################################
	// #########################################################################################################################################

	/**
	 * {@link Frame#getPositionX()}
	 * 
	 * @return the Distance of Window left border in pixels from left border of the
	 *         containing (main window).or -1 if unspecified
	 */
	protected int getFramePositionX() {
		return getView().getPositionX();
	}

	/**
	 * {@link Frame#setPositionX(int)}
	 * 
	 * @param x
	 *            the Distance of Window left border in pixels from left border of
	 *            the containing (main window). or -1 if unspecified.
	 */
	protected void setFramePositionX(int x) {
		getView().setPositionX(x);
	}

	/**
	 * {@link Frame#getPositionY()}
	 * 
	 * @return Distance of Window top border in pixels from top border of the
	 *         containing (main window). or -1 if unspecified
	 */
	protected int getFramePositionY() {
		return getView().getPositionY();
	}

	/**
	 * {@link Frame#setPositionY(int)}
	 * 
	 * @param y
	 *            the Distance of Window top border in pixels from top border of the
	 *            containing (main window). or -1 if unspecified
	 */
	protected void setFramePositionY(int y) {
		getView().setPositionY(y);
	}

	/**
	 * {@link Frame#setPosition(int, int)}
	 * 
	 * @param x
	 *            The new x coordinate for the window
	 * @param y
	 *            The new y coordinate for the window
	 */
	protected void setFramePosition(int x, int y) {
		getView().setPosition(x, y);
	}

	/**
	 * {@link Frame#center()}
	 */
	protected void centerFrame() {
		getView().center();
	}
}
