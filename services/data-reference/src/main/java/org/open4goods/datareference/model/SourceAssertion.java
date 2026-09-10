package org.open4goods.datareference.model;

import java.util.Objects;

/**
 * One immutable raw assertion made by a provider snapshot.
 *
 * @param field versioned provider field identity
 * @param rawValue unmodified provider value
 * @param rawUnit unmodified provider unit, or {@code null} when absent
 * @param language BCP 47 language tag or {@code und}
 * @param ordinal zero-based position among values of the same field
 * @param gtinMatchConfidence confidence attaching the source record to the GTIN
 */
public record SourceAssertion(
        SourceFieldId field,
        String rawValue,
        String rawUnit,
        LanguageTag language,
        int ordinal,
        GtinMatchConfidence gtinMatchConfidence) {

    /**
     * Validates mandatory provenance and ordering fields without altering raw data.
     */
    public SourceAssertion {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(rawValue, "rawValue must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(gtinMatchConfidence, "gtinMatchConfidence must not be null");
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be non-negative");
        }
    }
}
