package com.mantledillusion.vaadin.cotton;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

/**
 * Represents a URL (possibly with query parameters) to navigate to.
 */
public final class NavigationTarget {

	/**
	 * Builder for {@link NavigationTarget}s.
	 */
	public static final class QueryParametersBuilder {

		private final String url;
		private final Map<String, String[]> params = new HashMap<>();

		private QueryParametersBuilder(String url) {
			this.url = url;
		}

		/**
		 * Adds the given query parameter to the target to build.
		 * 
		 * @param key
		 *            The query parameter key; might <b>not</b> be null.
		 * @param values
		 *            The query parameter values; might <b>not</b> be null or empty
		 *            without contained nulls.
		 * @return this
		 */
		public QueryParametersBuilder add(String key, String... values) {
			if (StringUtils.isEmpty(key)) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot add a query parameter with an empty key.");
			}
			values = ArrayUtils.removeElements(values, (String) null);
			if (values.length == 0) {
				throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
						"Cannot add a query parameter without values.");
			}
			this.params.put(key, ArrayUtils.clone(values));
			return this;
		}

		/**
		 * Builds a {@link NavigationTarget} of the specifications currently contained
		 * in this builder.
		 * 
		 * @return A new {@link NavigationTarget}; never null
		 */
		public NavigationTarget build() {
			return new NavigationTarget(this.url, Collections.unmodifiableMap(new HashMap<>(this.params)));
		}
	}

	private final String url;
	private final Map<String, String[]> params;

	private NavigationTarget(String url, Map<String, String[]> params) {
		this.url = url;
		this.params = params;
	}

	String getUrl() {
		return this.url;
	}

	Map<String, String[]> getParams() {
		return this.params;
	}

	/**
	 * Convenience {@link Method} for building a query parameterless target.
	 * 
	 * @param url
	 *            The URL to navigate to; might <b>not</b> be null.
	 * @return A new {@link NavigationTarget} instance; never null
	 */
	public static NavigationTarget of(String url) {
		return builder(url).build();
	}

	/**
	 * Returns a {@link QueryParametersBuilder} for building a target with query
	 * parameters.
	 * 
	 * @param url
	 *            The URL to navigate to; might <b>not</b> be null.
	 * @return A new {@link QueryParametersBuilder}; never null
	 */
	public static QueryParametersBuilder builder(String url) {
		if (url == null) {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR,
					"Unable to create a navigaion target with a null url.");
		}
		return new QueryParametersBuilder(url);
	}
}
