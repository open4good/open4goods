package org.open4goods.datareference.model.normalization;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.model.LanguageTag;

/** Locale-output lookup for already normalized localized values. */
public final class LocalizedValueLookup {

    private LocalizedValueLookup() {
    }

    /**
     * Resolves text in exact-tag, parent-language, English and canonical-id order.
     *
     * @param values values keyed by their stored language tag
     * @param requested requested output language
     * @param canonicalId final stable fallback when no translation exists
     * @return localized output without altering the stored language or UCUM data
     */
    public static String resolve(Map<LanguageTag, String> values, LanguageTag requested, String canonicalId) {
        Objects.requireNonNull(values, "values must not be null");
        Objects.requireNonNull(requested, "requested must not be null");
        Objects.requireNonNull(canonicalId, "canonicalId must not be null");
        String exact = values.get(requested);
        if (exact != null) {
            return exact;
        }
        String parent = Locale.forLanguageTag(requested.value()).getLanguage();
        if (!parent.isEmpty()) {
            String parentValue = values.get(new LanguageTag(parent));
            if (parentValue != null) {
                return parentValue;
            }
        }
        String english = values.get(new LanguageTag("en"));
        return english != null ? english : canonicalId;
    }
}
