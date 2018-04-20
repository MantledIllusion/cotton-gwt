package com.mantledillusion.vaadin.cotton.environment.events.navigation;

import com.mantledillusion.vaadin.cotton.QueryParam;
import com.mantledillusion.vaadin.cotton.viewpresenter.Addressed;

/**
 * The types of navigation changes that are possible.
 */
public enum NavigationType {

    /**
     * Change type of URL -&gt; another {@link Addressed}.
     */
    SEGMENT_CHANGE,

    /**
     * Change type of {@link QueryParam} value -&gt; new {@link QueryParam} value on
     * the same URL.
     */
    QUERY_PARAM_CHANGE,

    /**
     * Refresh on the same URL.
     */
    REFRESH;
}
