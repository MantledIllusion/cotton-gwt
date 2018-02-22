package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.mantledillusion.essentials.reflection.TypeEssentials;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Addressable;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

/**
 * Registry that basically holds URL-&gt;{@link TypedBlueprint} mappings to
 * determine which {@link TypedBlueprint} to use for view injection when the
 * user visits a specific URL.
 * <p>
 * In addition, the registry can also handle certain special cases like Http410
 * GONE resources and Http30X redirects.
 */
public final class UrlResourceRegistry {

	private static final String URL_SEGMENT_REGEX = "[a-zA-Z0-9_]+";
	public static final String URL_PATH_REGEX = "(" + URL_SEGMENT_REGEX + "(/" + URL_SEGMENT_REGEX + ")*)?";

	private final Map<String, UrlResource> resourceRegistry = new HashMap<>();
	private final Map<String, String> redirectRegistry = new HashMap<>();

	private abstract class UrlResource {

		private final boolean isGone;
		private final boolean isView;
		private final Set<String> redirectTargetOf = new HashSet<>();

		public UrlResource(boolean isGone, boolean isView) {
			this.isGone = isGone;
			this.isView = isView;
		}

		abstract TypedBlueprint<? extends View> getViewResource();
	}

	private final class GoneResource extends UrlResource {

		public GoneResource() {
			super(true, false);
		}

		@Override
		TypedBlueprint<? extends View> getViewResource() {
			throw new WebException(HttpErrorCodes.HTTP903_NOT_IMPLEMENTED_ERROR,
					"There can never be a view resource at a Http410 GONE URL.");
		}
	}

	private final class ViewResource extends UrlResource {

		private final TypedBlueprint<? extends View> viewBlueprint;

		public ViewResource(TypedBlueprint<? extends View> viewResource) {
			super(false, true);
			this.viewBlueprint = viewResource;
		}

		@Override
		TypedBlueprint<? extends View> getViewResource() {
			return this.viewBlueprint;
		}
	}

	/**
	 * Returns whether there is a redirect on the given URL.
	 * 
	 * @param urlPath
	 *            The URL path to check; may be null, although the {@link Method}
	 *            will always return false in that case.
	 * @return True if there is a redirect registered at that URL, false otherwise
	 */
	public boolean hasRedirectAt(String urlPath) {
		return this.redirectRegistry.containsKey(urlPath);
	}

	/**
	 * Returns whether there is a view resource registered for the given URL.
	 * 
	 * @param urlPath
	 *            The URL path to check; may be null, although the {@link Method}
	 *            will always return false in that case.
	 * @return True if there is a view resource registered at that URL, false
	 *         otherwise
	 */
	public boolean hasViewAt(String urlPath) {
		return this.resourceRegistry.containsKey(urlPath) && this.resourceRegistry.get(urlPath).isView;
	}

	/**
	 * Returns whether there once was a view resource registered at the given URL,
	 * but isn't anymore.
	 * 
	 * @param urlPath
	 *            The URL path to check; may be null, although the {@link Method}
	 *            will always return false in that case.
	 * @return True if there once was a view resource registered at that URL and is
	 *         missing now, false otherwise
	 */
	public boolean hasGoneAt(String urlPath) {
		return this.resourceRegistry.containsKey(urlPath) && this.resourceRegistry.get(urlPath).isGone;
	}

	/**
	 * Redirects the given URL until there is no more redirect registered for the
	 * result, then returns it.
	 * 
	 * @param urlPath
	 *            The URL path to check; may be null, although the {@link Method}
	 *            will just return it since there can never be a redirect for a null
	 *            URL.
	 * @return The redirected URL; might be the unchanged given URL if it is null or
	 *         there is no redirect registered for that URL
	 */
	public String getRedirectAt(String urlPath) {
		while (this.redirectRegistry.containsKey(urlPath)) {
			urlPath = this.redirectRegistry.get(urlPath);
		}
		return urlPath;
	}

	/**
	 * Returns the {@link TypedBlueprint} registered for view injection at the given
	 * URL.
	 * 
	 * @param urlPath
	 *            The URL path to retrieve the {@link TypedBlueprint} for; might
	 *            <b>not</b> be null.
	 * @return The registered {@link TypedBlueprint} for the URL; never null
	 * @throws WebException
	 *             If there is no view resource registered at the given URL; check
	 *             using {@link #hasViewAt(String)}
	 */
	public TypedBlueprint<? extends View> getViewAt(String urlPath) {
		if (!hasViewAt(urlPath)) {
			throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
					"There is no view resource registered at url '" + urlPath + "'.");
		}
		return this.resourceRegistry.get(urlPath).getViewResource();
	}

	/**
	 * Registers the given {@link TypedBlueprint}'s root type {@link View}
	 * implementation at the URL in the view's @{@link Addressable} annotation.
	 * 
	 * @param viewBlueprint
	 *            The {@link TypedBlueprint} whose view to register; might
	 *            <b>not</b> be null, also the view has to be annotated
	 *            with @{@link Addressable} somewhere, view the documentation of
	 *            {@link Addressable} for reference.
	 */
	public void registerViewResource(TypedBlueprint<? extends View> viewBlueprint) {
		if (viewBlueprint == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot register a null blueprint as view resoruce.");
		}
		List<Class<?>> urls = TypeEssentials.getSuperClassesAnnotatedWith(viewBlueprint.getRootType(),
				Addressable.class);

		if (urls.isEmpty()) {
			throw new WebException(HttpErrorCodes.HTTP905_ILLEGAL_INTERFACE_USE,
					"Views that are registered as resources have to be annotated with @"
							+ Addressable.class.getSimpleName()
							+ " somewhere along its classes' hierarchy; however, the view type "
							+ viewBlueprint.getRootType().getSimpleName() + " isn't.");
		}

		Addressable url = urls.stream().skip(urls.size() - 1).findFirst().get().getAnnotation(Addressable.class);

		register(url.value(), new ViewResource(viewBlueprint));

		for (Addressable.Redirect redirect : url.redirects()) {
			registerRedirect(redirect.value(), url.value());
		}
	}

	private void registerRedirect(String urlPath, String redirectUrlPath) {
		checkUrlTarget(urlPath);

		List<String> visitedUrls = new ArrayList<>(Arrays.asList(urlPath, redirectUrlPath));
		String currentUrl = redirectUrlPath;
		while (this.redirectRegistry.containsKey(currentUrl)) {
			currentUrl = this.redirectRegistry.get(currentUrl);
			visitedUrls.add(currentUrl);
			if (urlPath.equals(currentUrl)) {
				throw new WebException(HttpErrorCodes.HTTP508_LOOP_DETECTED,
						"Redirecting the URL '" + urlPath + "' to the URL '" + redirectUrlPath
								+ "' would cause a circlic redirection: " + StringUtils.join(visitedUrls, " -> "));
			}
		}

		if (!this.redirectRegistry.containsKey(redirectUrlPath)) {
			this.resourceRegistry.get(redirectUrlPath).redirectTargetOf.add(redirectUrlPath);
		}
		this.redirectRegistry.put(urlPath, redirectUrlPath);
	}

	/**
	 * Returns whether there once was a view resource registered at the given URL,
	 * but there is no redirect to a new location so the server should throw a
	 * {@link WebException} with {@link HttpErrorCodes#HTTP410_GONE} when the URL is
	 * visited.
	 * 
	 * @param urlPath
	 *            Path to register the GONE resource at; might <b>not</b> be null
	 *            and has to match {@link #URL_PATH_REGEX}
	 */
	public void registerGoneResource(String urlPath) {
		UrlResourceRegistry.checkUrlPattern(urlPath);
		register(urlPath, new GoneResource());
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

	private void register(String url, UrlResource resource) {
		checkUrlTarget(url);
		this.resourceRegistry.put(url, resource);
	}

	private void checkUrlTarget(String url) {
		if (this.redirectRegistry.containsKey(url)) {
			throw new WebException(HttpErrorCodes.HTTP907_ILLEGAL_STRUCTURING,
					"Cannot register the resource at url '" + url + "'; a redirect to '"
							+ this.redirectRegistry.get(url) + "' is already registered at that path.");
		} else if (this.resourceRegistry.containsKey(url)) {
			throw new WebException(HttpErrorCodes.HTTP907_ILLEGAL_STRUCTURING,
					"Cannot register the resource at url '" + url + "'; a "
							+ (this.resourceRegistry.get(url).isView ? "view" : "gone")
							+ " resource is already registered at that path.");
		}
	}
}
