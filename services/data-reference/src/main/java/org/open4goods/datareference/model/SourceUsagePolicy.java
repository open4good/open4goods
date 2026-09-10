package org.open4goods.datareference.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Reviewed permission to publish one source's content, on named surfaces, for a
 * named period.
 *
 * <p>Deny by default: {@link #allows} answers {@code true} only when the source,
 * the content type, the surface and the instant were all explicitly approved.
 * There is no wildcard and no inheritance, because the failure mode being
 * guarded against is publishing licensed content that nobody decided to publish.
 *
 * <p>Content type is part of the key rather than a detail. A source commonly
 * permits republishing its classifications while forbidding its texts and
 * images, and a policy that could only speak about a whole source would have to
 * take the most restrictive reading of all of them.
 *
 * @param policyId stable policy identifier
 * @param sourceId source governed by the policy
 * @param version policy version
 * @param contentTypes content types this policy speaks about; empty covers none
 * @param allowedSurfaces explicitly allowed projection surfaces; empty denies all
 * @param effectiveFrom first instant the policy applies
 * @param effectiveUntil last instant the policy applies, or {@code null} when open-ended
 * @param retention maximum retention of source assertions under this policy
 * @param mediaCache cache restrictions for media and provider content
 * @param attribution publication attribution requirement
 * @param redistribution redistribution permission
 * @param legalReviewDate date of the legal review supporting this version
 */
public record SourceUsagePolicy(
        String policyId,
        SourceId sourceId,
        String version,
        Set<SourceContentType> contentTypes,
        Set<ProjectionSurface> allowedSurfaces,
        Instant effectiveFrom,
        Instant effectiveUntil,
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
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        version = requireText(version, "version");
        contentTypes = contentTypes == null ? Set.of() : Set.copyOf(contentTypes);
        allowedSurfaces = allowedSurfaces == null ? Set.of() : Set.copyOf(allowedSurfaces);
        Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        if (effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveUntil must not precede effectiveFrom");
        }
        if (retention == null || retention.isNegative()) {
            throw new IllegalArgumentException("retention must be non-negative");
        }
        Objects.requireNonNull(mediaCache, "mediaCache must not be null");
        Objects.requireNonNull(attribution, "attribution must not be null");
        Objects.requireNonNull(redistribution, "redistribution must not be null");
        Objects.requireNonNull(legalReviewDate, "legalReviewDate must not be null");
    }

    /**
     * Builds a policy that permits nothing, for a source with no reviewed terms.
     *
     * @param policyId stable policy identifier
     * @param sourceId source identifier
     * @param version policy version
     * @param effectiveFrom first instant the policy applies
     * @param legalReviewDate review date
     * @return policy allowing no surface, no content type and no retention
     */
    public static SourceUsagePolicy denyAll(
            String policyId,
            SourceId sourceId,
            String version,
            Instant effectiveFrom,
            LocalDate legalReviewDate) {
        return new SourceUsagePolicy(
                policyId,
                sourceId,
                version,
                Set.of(),
                Set.of(),
                effectiveFrom,
                null,
                Duration.ZERO,
                MediaCachePolicy.NONE,
                AttributionRequirement.NONE,
                RedistributionPolicy.PROHIBITED,
                legalReviewDate);
    }

    /**
     * Reports whether this policy explicitly permits a publication.
     *
     * @param contentType content type being published
     * @param surface surface it would be published on
     * @param instant instant of publication
     * @return {@code true} only when all four coordinates were explicitly approved
     */
    public boolean allows(SourceContentType contentType, ProjectionSurface surface, Instant instant) {
        if (contentType == null || surface == null || instant == null) {
            return false;
        }
        return contentTypes.contains(contentType)
                && allowedSurfaces.contains(surface)
                && isEffectiveAt(instant);
    }

    /**
     * Reports whether the policy is in force at an instant.
     *
     * @param instant instant to test
     * @return {@code true} when the instant falls in the effective interval
     */
    public boolean isEffectiveAt(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return !instant.isBefore(effectiveFrom) && (effectiveUntil == null || !instant.isAfter(effectiveUntil));
    }

    /**
     * Returns the immutable reference stored by record heads.
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
