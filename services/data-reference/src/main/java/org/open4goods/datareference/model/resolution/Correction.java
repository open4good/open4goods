package org.open4goods.datareference.model.resolution;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import org.open4goods.datareference.model.AssertionId;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
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
        AssertionId assertionId,
        Gtin gtin,
        CanonicalAttributeId attribute,
        CanonicalValue value,
        Instant reviewedAt,
        String reviewedBy,
        String reason,
        Set<ProjectionSurface> surfaces,
        Instant revokedAt) {

    /**
     * Validates the correction.
     */
    public Correction {
        Objects.requireNonNull(assertionId, "assertionId must not be null");
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(attribute, "attribute must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        Objects.requireNonNull(reviewedBy, "reviewedBy must not be null");
        if (reviewedBy.isBlank()) {
            throw new IllegalArgumentException("reviewedBy must not be blank");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        surfaces = Set.copyOf(Objects.requireNonNull(surfaces, "surfaces must not be null"));
        if (surfaces.isEmpty()) {
            throw new IllegalArgumentException("surfaces must not be empty");
        }
        if (revokedAt != null && revokedAt.isBefore(reviewedAt)) {
            throw new IllegalArgumentException("revokedAt must not precede reviewedAt");
        }
    }

    /**
     * Compatibility constructor for pre-scope corrections. New producers must
     * provide an immutable assertion id, reason and publication scope.
     */
    public Correction(Gtin gtin, CanonicalAttributeId attribute, CanonicalValue value, Instant reviewedAt, String reviewedBy) {
        this(new AssertionId("o4g-correction:" + gtin.value() + ":" + attribute.externalForm() + ":" + reviewedAt.toEpochMilli()),
                gtin, attribute, value, reviewedAt, reviewedBy, "legacy reviewed correction", Set.of(ProjectionSurface.values()), null);
    }

    /** Reports whether this immutable correction remains valid on a surface. */
    public boolean appliesTo(ProjectionSurface surface, Instant instant) {
        return surfaces.contains(Objects.requireNonNull(surface, "surface must not be null"))
                && (revokedAt == null || instant.isBefore(revokedAt));
    }
}
