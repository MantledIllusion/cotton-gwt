package com.mantledillusion.vaadin.cotton.testsuites.viewpresenter.working;

import java.util.EventObject;

import com.mantledillusion.vaadin.cotton.viewpresenter.View.Present;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

@Present(PresenterA.class)
public class ViewA extends View {

	private static final long serialVersionUID = 1L;
	
	private static class UnimportantEvent extends EventObject {

		private static final long serialVersionUID = 1L;

		public UnimportantEvent(Object source) {
			super(source);
		}
		
	}
	
	private static class ModifiedButton extends Button {
		
		private static final long serialVersionUID = 1L;

		public void fireUnimportantEvent() {
			fireEvent(new UnimportantEvent(this));
		}
	}

	static final String COMPONENT_ID = "componentId";

	private final ModifiedButton b = new ModifiedButton();
	
	@Override
	protected Component buildUI(TemporalActiveComponentRegistry reg) throws Exception {
		reg.registerActiveComponent(COMPONENT_ID, this.b);
		return this.b;
	}
	
	public void fireImportantEvent() {
		this.b.click();
	}
	
	public void fireUnimportantEvent() {
		this.b.fireUnimportantEvent();
	}
}
