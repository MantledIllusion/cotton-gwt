package com.mantledillusion.vaadin.cotton;

import java.lang.annotation.Annotation;
import java.util.List;

import com.mantledillusion.essentials.reflection.TypeEssentials;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Addressed;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

public class WebUtils {

	private static final String URL_SEGMENT_REGEX = "[a-zA-Z0-9_]+";
	public static final String URL_PATH_REGEX = "(" + URL_SEGMENT_REGEX + "(/" + URL_SEGMENT_REGEX + ")*)?";

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

	/**
	 * Checks whether the given URL is not null and matches the pattern for URL
	 * paths, which is {@link #URL_PATH_REGEX}.
	 * 
	 * @param url
	 *            The url to check; might <b>not</b> be null or not matching to the
	 *            pattern for valid URL paths.
	 * @throws WebException
	 *             Thrown if the given URL is null or no valid URL path
	 */
	public static void checkUrlPattern(String url) throws WebException {
		if (url == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot register a resource at a null url.");
		} else if (!url.matches(URL_PATH_REGEX)) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot register the resource at url '" + url
							+ "'; the url does not match the valid format for segmented url paths: " + URL_PATH_REGEX);
		}
	}
}
