package com.mantledillusion.vaadin.cotton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.mantledillusion.vaadin.cotton.exception.WebException;
import com.mantledillusion.vaadin.cotton.exception.WebException.HttpErrorCodes;

final class LocalizationResource {

	private interface Evaluateable {

		String evaluate(Map<String, Object> msgParameters);

		Evaluateable collapse();
	}

	private final class StaticEvaluateable implements Evaluateable {

		private final String content;

		private StaticEvaluateable(String content) {
			this.content = content;
		}

		@Override
		public String evaluate(Map<String, Object> msgParameters) {
			return replaceOrLocalize(this.content, msgParameters);
		}

		@Override
		public Evaluateable collapse() {
			return this;
		}
	}

	private final class DynamicEvaluateable implements Evaluateable {

		private final List<Evaluateable> content = new ArrayList<>();

		@Override
		public String evaluate(Map<String, Object> msgParameters) {
			StringBuilder sb = new StringBuilder();
			this.content.forEach(evaluateable -> sb.append(evaluateable.evaluate(msgParameters)));
			return replaceOrLocalize(sb.toString(), msgParameters);
		}

		@Override
		public Evaluateable collapse() {
			if (this.content.size() == 1) {
				return this.content.get(0).collapse();
			} else {
				for (int i = 0; i < this.content.size(); i++) {
					this.content.set(i, this.content.get(i).collapse());
				}
				return this;
			}
		}
	}

	private final Locale locale;
	private final Map<String, ResourceBundle> bundles = new HashMap<>();
	private final Map<String, Evaluateable> evaluateables = new HashMap<>();

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

	boolean hasLocalization(String msgId) {
		return this.bundles.containsKey(msgId);
	}

	String renderMessage(String msgId, Map<String, Object> namedMsgParameters, Object... indexedMsgParameters) {
		if (this.bundles.containsKey(msgId)) {
			Map<String, Object> params = new HashMap<>(
					ObjectUtils.defaultIfNull(namedMsgParameters, Collections.emptyMap()));
			indexedMsgParameters = ObjectUtils.defaultIfNull(indexedMsgParameters, ArrayUtils.EMPTY_OBJECT_ARRAY);
			for (int i = 0; i < indexedMsgParameters.length; i++) {
				params.put(String.valueOf(i), indexedMsgParameters[i]);
			}

			if (!this.evaluateables.containsKey(msgId)) {
				this.evaluateables.put(msgId, createEvaluteable(msgId, this.bundles.get(msgId).getString(msgId)));
			}
			return this.evaluateables.get(msgId).evaluate(params);
		} else if (msgId.matches(CottonUI.REGEX_TYPICAL_MESSAGE_ID)) {
			CottonUI.LOGGER.warn("Unable to localize '" + msgId + "' with bundle of language '"
					+ locale.getISO3Language() + "': msgId is not matching any resource key.");
		}
		return msgId;
	}

	private Evaluateable createEvaluteable(String msgId, String unevaluatedMsg) {
		if (StringUtils.countMatches(unevaluatedMsg, '{') != StringUtils.countMatches(unevaluatedMsg, '}')) {
			CottonUI.LOGGER.warn("Unable to localize '" + msgId + "' with bundle of language '"
					+ locale.getISO3Language() + "': the message '" + unevaluatedMsg
					+ "' is malformatted; it does not contain the same amount of '{' as it does '}'.");
			return new StaticEvaluateable(unevaluatedMsg);
		}
		DynamicEvaluateable rootEvaluateable = new DynamicEvaluateable();
		evaluateInto(rootEvaluateable, unevaluatedMsg);
		return rootEvaluateable.collapse();
	}

	private String evaluateInto(DynamicEvaluateable currentLevelEvaluateable, String msgRest) {
		while (!msgRest.isEmpty()) {
			if (msgRest.charAt(0) == '{') {
				DynamicEvaluateable lowerLevelEvaluateable = new DynamicEvaluateable();
				currentLevelEvaluateable.content.add(lowerLevelEvaluateable);
				msgRest = evaluateInto(lowerLevelEvaluateable, msgRest.substring(1));
			} else if (msgRest.charAt(0) == '}') {
				return msgRest.substring(1);
			} else {
				int nextOpening = msgRest.indexOf('{');
				int nextClosing = msgRest.indexOf('}');
				if (nextOpening == nextClosing) { // CAN ONLY BE IN -1 CASE
					currentLevelEvaluateable.content.add(new StaticEvaluateable(msgRest));
					return StringUtils.EMPTY;
				} else {
					nextOpening = nextOpening == -1 ? Integer.MAX_VALUE : nextOpening;
					nextClosing = nextClosing == -1 ? Integer.MAX_VALUE : nextClosing;
					String content = msgRest.substring(0, Math.min(nextOpening, nextClosing));
					currentLevelEvaluateable.content.add(new StaticEvaluateable(content));
					msgRest = msgRest.substring(content.length());
				}
			}
		}
		return StringUtils.EMPTY;
	}

	private String replaceOrLocalize(String msgId, Map<String, Object> msgParameters) {
		if (msgParameters.containsKey(msgId)) {
			return Objects.toString(msgParameters.get(msgId));
		} else if (this.bundles.containsKey(msgId)) {
			return this.bundles.get(msgId).getString(msgId);
		} else if (msgId.matches(CottonUI.REGEX_TYPICAL_MESSAGE_ID)) {
			CottonUI.LOGGER.warn("Unable to localize '" + msgId + "' with bundle of language '"
					+ locale.getISO3Language() + "': msgId is not matching any resource key.");
		}
		return msgId;
	}
}