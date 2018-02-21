package com.mantledillusion.vaadin.cotton;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;

import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

final class LocalizationResource {

	private final Locale locale;
	private final Map<String, ResourceBundle> bundles = new HashMap<>();
	private final Map<String, MessageFormat> formats = new HashMap<>();

	LocalizationResource(Locale locale) {
		this.locale = locale;
	}

	void addBundle(ResourceBundle bundle, Set<String> bundleKeys) {
		Set<String> intersection = SetUtils.intersection(bundles.keySet(), bundleKeys);
		if (intersection.isEmpty()) {
			MapUtils.populateMap(this.bundles, bundleKeys, key -> key, value -> bundle);
		} else {
			throw new WebException(HttpErrorCodes.HTTP901_ILLEGAL_ARGUMENT_ERROR, "The resource bundle "
					+ bundle.getBaseBundleName()
					+ " shares the following message ids with other bundles of the same language, which is forbidden: "
					+ Arrays.toString(intersection.stream()
							.map(key -> "'" + key + "' (also in " + this.bundles.get(key).getBaseBundleName() + ")")
							.toArray()));
		}
	}

	String renderMessage(String msgId, Object... params) {
		if (this.bundles.containsKey(msgId)) {
			if (!this.formats.containsKey(msgId)) {
				this.formats.put(msgId, new MessageFormat(this.bundles.get(msgId).getString(msgId), this.locale));
			}
			return this.formats.get(msgId).format(params);
		} else if (msgId.matches(CottonUI.REGEX_TYPICAL_MESSAGE_ID)) {
			CottonUI.LOGGER.warn("Unable to localize '" + msgId + "'; with bundle of language '"
					+ locale.getISO3Language() + "'.");
		}
		return msgId;
	}
}