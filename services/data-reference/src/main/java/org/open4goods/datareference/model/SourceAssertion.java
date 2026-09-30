package org.open4goods.datareference.model;

import java.util.Objects;

import org.open4goods.datareference.model.evidence.SourceEvidence;

/**
 * One immutable thing a source said about a product, at one coordinate.
 *
 * <p>The coordinate is {@code (field, ordinal)}: a source that lists three
 * bullet points states three assertions on the same field, distinguished by
 * ordinal, and that ordinal is provider order, not a ranking. The evidence is a
 * sealed payload rather than a string, so a media reference cannot be silently
 * stored as text and a provider unit cannot be lost by concatenation.
 *
 * <p>{@code contentType} is what a licence reasons about; it is stated per
 * assertion rather than per record because one provider record routinely mixes
 * an identity field, an attribute and an image under different terms.
 *
 * @param assertionId stable identity derived from the record, field and ordinal
 * @param field versioned provider field identity
 * @param ordinal zero-based position among values of the same field
 * @param contentType kind of content, for usage-policy filtering
 * @param evidence what the source actually said
 */
public record SourceAssertion(
        AssertionId assertionId,
        SourceFieldId field,
        int ordinal,
        SourceContentType contentType,
        SourceEvidence evidence) {

    /**
     * Validates the assertion coordinate and its payload.
     */
    public SourceAssertion {
        Objects.requireNonNull(assertionId, "assertionId must not be null");
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be non-negative");
        }
    }

    /**
     * Builds an assertion whose identity is derived from its own coordinate.
     *
     * @param recordKey record the assertion belongs to
     * @param field versioned provider field
     * @param ordinal zero-based position among values of that field
     * @param contentType kind of content
     * @param evidence what the source said
     * @return assertion carrying a derived, stable identifier
     */
    public static SourceAssertion of(
            SourceRecordKey recordKey,
            SourceFieldId field,
            int ordinal,
            SourceContentType contentType,
            SourceEvidence evidence) {
        return new SourceAssertion(
                AssertionId.of(recordKey, field, ordinal), field, ordinal, contentType, evidence);
    }

    /**
     * Returns the coordinate this assertion occupies inside its record.
     *
     * @return field and ordinal pair
     */
    public Coordinate coordinate() {
        return new Coordinate(field, ordinal);
    }

    /**
     * Position of one assertion inside its record.
     *
     * @param field versioned provider field
     * @param ordinal zero-based position among values of that field
     */
    public record Coordinate(SourceFieldId field, int ordinal) {
    }
}
