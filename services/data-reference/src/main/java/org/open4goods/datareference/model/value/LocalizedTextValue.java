package org.open4goods.datareference.model.value;

import java.util.Objects;

import org.open4goods.datareference.model.LanguageTag;

/**
 * Localized canonical text.
 *
 * @param value text value
 * @param language actual BCP 47 language, or {@code und}
 */
public record LocalizedTextValue(String value, LanguageTag language) implements CanonicalValue {

    /**
     * Validates the text and its explicit language.
     */
    public LocalizedTextValue {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(language, "language must not be null");
    }

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.LOCALIZED_TEXT;
    }
}
