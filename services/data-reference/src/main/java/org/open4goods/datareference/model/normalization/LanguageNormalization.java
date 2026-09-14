package org.open4goods.datareference.model.normalization;

import org.open4goods.datareference.model.LanguageTag;

/** Result of normalizing an optional provider language declaration. */
public record LanguageNormalization(LanguageTag language, String diagnostic) {

    /**
     * Normalizes BCP 47 input without assigning a serving-domain language.
     *
     * @param providerLanguage provider language tag, or {@code null}
     * @return a valid tag, or {@code und} with an auditable diagnostic
     */
    public static LanguageNormalization fromProvider(String providerLanguage) {
        if (providerLanguage == null || providerLanguage.isBlank()) {
            return new LanguageNormalization(LanguageTag.UND, "provider language is absent");
        }
        try {
            return new LanguageNormalization(new LanguageTag(providerLanguage), null);
        } catch (IllegalArgumentException exception) {
            return new LanguageNormalization(LanguageTag.UND, "invalid provider language: " + providerLanguage);
        }
    }
}
