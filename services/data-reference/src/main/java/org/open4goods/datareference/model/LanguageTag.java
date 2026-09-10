package org.open4goods.datareference.model;

import java.util.IllformedLocaleException;
import java.util.Locale;

/**
 * Canonical BCP 47 language tag carried by every source assertion.
 *
 * @param value canonical BCP 47 tag or {@code und} for non-linguistic data
 */
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record LanguageTag(@JsonValue String value) {

    /** Language tag for non-linguistic or genuinely unknown content. */
    public static final LanguageTag UND = new LanguageTag("und");

    /**
     * Validates and canonicalizes the supplied BCP 47 tag.
     */
    public LanguageTag {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Language tag must be explicit; use 'und' when unknown");
        }
        try {
            value = new Locale.Builder().setLanguageTag(value.trim()).build().toLanguageTag();
        } catch (IllformedLocaleException exception) {
            throw new IllegalArgumentException("Invalid BCP 47 language tag: " + value, exception);
        }
    }

    /**
     * Rebuilds the identifier from its serialized form.
     *
     * @param value serialized value
     * @return validated identifier
     */
    @JsonCreator
    public static LanguageTag fromJson(String value) {
        return new LanguageTag(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
