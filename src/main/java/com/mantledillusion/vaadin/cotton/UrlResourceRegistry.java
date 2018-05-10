package com.mantledillusion.vaadin.cotton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.mantledillusion.injection.hura.Blueprint;
import com.mantledillusion.injection.hura.Blueprint.TypedBlueprint;
import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;
import com.mantledillusion.vaadin.cotton.viewpresenter.Addressed;
import com.mantledillusion.vaadin.cotton.viewpresenter.View;

final class UrlResourceRegistry {

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

	boolean hasRedirectAt(String urlPath) {
		return this.redirectRegistry.containsKey(urlPath);
	}

	boolean hasViewAt(String urlPath) {
		return this.resourceRegistry.containsKey(urlPath) && this.resourceRegistry.get(urlPath).isView;
	}

	boolean hasGoneAt(String urlPath) {
		return this.resourceRegistry.containsKey(urlPath) && this.resourceRegistry.get(urlPath).isGone;
	}

	String getRedirectAt(String urlPath) {
		while (this.redirectRegistry.containsKey(urlPath)) {
			urlPath = this.redirectRegistry.get(urlPath);
		}
		return urlPath;
	}

	TypedBlueprint<? extends View> getViewAt(String urlPath) {
		if (!hasViewAt(urlPath)) {
			throw new WebException(HttpErrorCodes.HTTP902_ILLEGAL_STATE_ERROR,
					"There is no view resource registered at url '" + urlPath + "'.");
		}
		return this.resourceRegistry.get(urlPath).getViewResource();
	}

	void registerViewResource(Class<? extends View> viewClass) {
		registerViewResource(Blueprint.of(viewClass));
	}

	void registerViewResource(TypedBlueprint<? extends View> viewBlueprint) {
		if (viewBlueprint == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Cannot register a null blueprint as view resoruce.");
		}

		Addressed url = WebUtils.getAddressFrom(viewBlueprint.getRootType());

		register(url.value(), new ViewResource(viewBlueprint));

		for (Addressed.Redirect redirect : url.redirects()) {
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

	void registerGoneResource(String urlPath) {
		WebUtils.checkUrlPattern(urlPath);
		register(urlPath, new GoneResource());
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
