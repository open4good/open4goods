package org.open4goods.datareference.model.resolution;

import java.time.Instant;
import java.util.Objects;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.value.CanonicalValue;

/**
 * A reviewed O4G decision that overrides sources in a declared scope.
 *
 * <p>A correction is evidence O4G itself produced, so it is versioned and dated
 * like any other: an override with no author and no review date cannot be
 * revisited when the source it corrects is fixed upstream.
 *
 * @param gtin product the correction applies to
 * @param attribute canonical attribute it overrides
 * @param value value to use instead
 * @param reviewedAt instant the correction was reviewed
 * @param reviewedBy operator-readable author reference
 */
public record Correction(
        Gtin gtin,
        CanonicalAttributeId attribute,
        CanonicalValue value,
        Instant reviewedAt,
        String reviewedBy) {

    /**
     * Validates the correction.
     */
    public Correction {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(attribute, "attribute must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        Objects.requireNonNull(reviewedBy, "reviewedBy must not be null");
        if (reviewedBy.isBlank()) {
            throw new IllegalArgumentException("reviewedBy must not be blank");
        }
    }
}
