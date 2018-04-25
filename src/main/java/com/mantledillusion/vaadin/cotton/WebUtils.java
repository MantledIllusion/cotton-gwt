package com.mantledillusion.vaadin.cotton;

import java.lang.annotation.Annotation;
import java.util.List;

import com.mantledillusion.essentials.reflection.TypeEssentials;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Addressed;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

public class WebUtils {

	/**
	 * Extracts the @{@link Addressed} {@link Annotation} from the given
	 * {@link View} implementation that it is addressed by.
	 * 
	 * @param viewClass
	 *            The {@link View} annotated with @{@link Addressed} to navigate to;
	 *            <b>not</b> allowed to be null.
	 * @return The {@link Addressed} annotation of the given {@link View}
	 *         implementation; never null
	 */
	public static Addressed getAddressFrom(Class<? extends View> viewClass) {
		if (viewClass == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Unable to navigate to a null addressed view.");
		}

		List<Class<?>> urls = TypeEssentials.getSuperClassesAnnotatedWith(viewClass, Addressed.class);

		if (urls.isEmpty()) {
			throw new WebException(HttpErrorCodes.HTTP905_ILLEGAL_INTERFACE_USE,
					"Views that should be addressed have to be annotated with @" + Addressed.class.getSimpleName()
							+ " somewhere along its classes' hierarchy; however, the view type "
							+ viewClass.getSimpleName() + " isn't.");
		}

		return urls.stream().skip(urls.size() - 1).findFirst().get().getAnnotation(Addressed.class);
	}
}
