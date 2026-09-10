package org.open4goods.datareference.model;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Reviewed usage policy applied before source assertions enter a projection.
 *
 * @param policyId stable policy identifier
 * @param source source governed by the policy
 * @param version policy version
 * @param allowedSurfaces explicitly allowed projection surfaces; empty denies all
 * @param retention maximum source assertion retention
 * @param mediaCache cache restrictions for media and provider content
 * @param attribution publication attribution requirement
 * @param redistribution redistribution permission
 * @param legalReviewDate date of the legal review supporting this version
 */
public record SourceUsagePolicy(
        String policyId,
        String source,
        String version,
        Set<ProjectionSurface> allowedSurfaces,
        Duration retention,
        MediaCachePolicy mediaCache,
        AttributionRequirement attribution,
        RedistributionPolicy redistribution,
        LocalDate legalReviewDate) {

    /**
     * Copies mutable inputs and enforces explicit, non-negative policy values.
     */
    public SourceUsagePolicy {
        policyId = requireText(policyId, "policyId");
        source = requireText(source, "source");
        version = requireText(version, "version");
        allowedSurfaces = allowedSurfaces == null ? Set.of() : Set.copyOf(allowedSurfaces);
        if (retention == null || retention.isNegative()) {
            throw new IllegalArgumentException("retention must be non-negative");
        }
        Objects.requireNonNull(mediaCache, "mediaCache must not be null");
        Objects.requireNonNull(attribution, "attribution must not be null");
        Objects.requireNonNull(redistribution, "redistribution must not be null");
        Objects.requireNonNull(legalReviewDate, "legalReviewDate must not be null");
    }

    /**
     * Builds a refusal-by-default policy for an unapproved source.
     *
     * @param policyId stable policy identifier
     * @param source source identifier
     * @param version policy version
     * @param legalReviewDate review date
     * @return policy allowing no projection or retained provider content
     */
    public static SourceUsagePolicy denyAll(
            String policyId, String source, String version, LocalDate legalReviewDate) {
        return new SourceUsagePolicy(
                policyId,
                source,
                version,
                Set.of(),
                Duration.ZERO,
                MediaCachePolicy.NONE,
                AttributionRequirement.NONE,
                RedistributionPolicy.PROHIBITED,
                legalReviewDate);
    }

    /**
     * Reports whether this reviewed policy explicitly allows a projection surface.
     *
     * @param surface requested surface
     * @return {@code true} only for an explicitly listed surface
     */
    public boolean allows(ProjectionSurface surface) {
        return surface != null && allowedSurfaces.contains(surface);
    }

    /**
     * Returns the immutable reference stored by snapshots.
     *
     * @return versioned policy reference
     */
    public SourceUsagePolicyRef reference() {
        return new SourceUsagePolicyRef(policyId, version);
    }

    /**
     * Validates a required policy string.
     *
     * @param value policy value
     * @param name field name
     * @return trimmed value
     */
    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
