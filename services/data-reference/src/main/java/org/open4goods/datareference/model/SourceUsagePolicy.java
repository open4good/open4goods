package org.open4goods.datareference.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
 * <p>Surface grants are keyed by content type rather than a cartesian product of
 * two flat sets, because the two do not vary independently: a source that
 * clears its identifiers for the ODbL export never clears its attributes or
 * text for the same surface, and a flat {@code contentTypes x allowedSurfaces}
 * pair cannot express that without a second policy record, which the
 * single-reference resolution model cannot evaluate against. A content type
 * present as a key, even with an empty surface set, is a content type the
 * policy speaks about for {@link #allowsUse}; a content type absent from the
 * map is not covered at all.
 *
 * @param policyId stable policy identifier
 * @param sourceId source governed by the policy
 * @param version policy version
 * @param surfaceGrants projection surfaces explicitly allowed per content type; a covered content
 *     type with an empty surface set is reviewed but published nowhere
 * @param effectiveFrom first instant the policy applies
 * @param effectiveUntil last instant the policy applies, or {@code null} when open-ended
 * @param retention maximum retention of source assertions under this policy
 * @param mediaCache cache restrictions for media and provider content
 * @param attribution publication attribution requirement
 * @param redistribution redistribution permission
 * @param derivativeLicence licence a derived work must carry; absent reads as {@link DerivativeLicence#NONE}
 * @param prohibitedUses uses this source forbids; absent reads as every {@link ProhibitedUse}
 * @param legalReviewDate date of the legal review supporting this version
 * @param reviewState whether an owner has reviewed the policy version
 * @param revokedAt first instant at which this version no longer permits publication, or {@code null}
 * @param evidenceReferences immutable references reviewed for this policy version
 */
public record SourceUsagePolicy(
        String policyId,
        SourceId sourceId,
        String version,
        Map<SourceContentType, Set<ProjectionSurface>> surfaceGrants,
        Instant effectiveFrom,
        Instant effectiveUntil,
        Duration retention,
        MediaCachePolicy mediaCache,
        AttributionRequirement attribution,
        RedistributionPolicy redistribution,
        DerivativeLicence derivativeLicence,
        Set<ProhibitedUse> prohibitedUses,
        LocalDate legalReviewDate,
        PolicyReviewState reviewState,
        Instant revokedAt,
        List<URI> evidenceReferences) {

    /**
     * Copies mutable inputs and enforces explicit, non-negative policy values.
     */
    public SourceUsagePolicy {
        policyId = requireText(policyId, "policyId");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        version = requireText(version, "version");
        surfaceGrants = copySurfaceGrants(surfaceGrants);
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
        derivativeLicence = derivativeLicence == null ? DerivativeLicence.NONE : derivativeLicence;
        prohibitedUses = prohibitedUses == null ? EnumSet.allOf(ProhibitedUse.class) : Set.copyOf(prohibitedUses);
        Objects.requireNonNull(legalReviewDate, "legalReviewDate must not be null");
        Objects.requireNonNull(reviewState, "reviewState must not be null");
        if (revokedAt != null && revokedAt.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("revokedAt must not precede effectiveFrom");
        }
        evidenceReferences = List.copyOf(Objects.requireNonNull(
                evidenceReferences, "evidenceReferences must not be null"));
        if (evidenceReferences.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("evidenceReferences must not contain null");
        }
        if (reviewState == PolicyReviewState.REVIEWED && evidenceReferences.isEmpty()) {
            throw new IllegalArgumentException("reviewed policy must have evidence references");
        }
    }

    /**
     * Creates an explicitly reviewed policy with the supplied evidence.
     *
     * @param policyId stable policy identifier
     * @param sourceId source governed by the policy
     * @param version policy version
     * @param surfaceGrants projection surfaces explicitly allowed per content type
     * @param effectiveFrom first instant the policy applies
     * @param effectiveUntil last instant the policy applies, or {@code null}
     * @param retention maximum retention of source assertions
     * @param mediaCache source media cache restrictions
     * @param attribution publication attribution requirement
     * @param redistribution redistribution permission
     * @param derivativeLicence licence a derived work must carry
     * @param prohibitedUses uses this source forbids
     * @param legalReviewDate review date
     * @param evidenceReferences reviewed evidence references
     */
    public SourceUsagePolicy(
            String policyId,
            SourceId sourceId,
            String version,
            Map<SourceContentType, Set<ProjectionSurface>> surfaceGrants,
            Instant effectiveFrom,
            Instant effectiveUntil,
            Duration retention,
            MediaCachePolicy mediaCache,
            AttributionRequirement attribution,
            RedistributionPolicy redistribution,
            DerivativeLicence derivativeLicence,
            Set<ProhibitedUse> prohibitedUses,
            LocalDate legalReviewDate,
            List<URI> evidenceReferences) {
        this(policyId, sourceId, version, surfaceGrants, effectiveFrom, effectiveUntil, retention,
                mediaCache, attribution, redistribution, derivativeLicence, prohibitedUses, legalReviewDate,
                PolicyReviewState.REVIEWED, null, evidenceReferences);
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
                Map.of(),
                effectiveFrom,
                null,
                Duration.ZERO,
                MediaCachePolicy.NONE,
                AttributionRequirement.NONE,
                RedistributionPolicy.PROHIBITED,
                DerivativeLicence.NONE,
                EnumSet.allOf(ProhibitedUse.class),
                legalReviewDate,
                PolicyReviewState.UNREVIEWED,
                null,
                List.of());
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
        return reviewState == PolicyReviewState.REVIEWED
                && redistribution != RedistributionPolicy.PROHIBITED
                && surfaceGrants.getOrDefault(contentType, Set.of()).contains(surface)
                && isEffectiveAt(instant);
    }

    /**
     * Reports whether this policy explicitly permits a named use of its content.
     *
     * <p>Mirrors {@link #allows}: an unreviewed policy or an instant outside the
     * effective interval denies exactly as it does there, and a use is permitted
     * only when the content type was reviewed for it and it is not listed in
     * {@link #prohibitedUses}.
     *
     * @param contentType content type the use would draw on
     * @param use named use being tested, such as AI training
     * @param instant instant of the proposed use
     * @return {@code true} only when the reviewed policy covers the content type and does not forbid the use
     */
    public boolean allowsUse(SourceContentType contentType, ProhibitedUse use, Instant instant) {
        if (contentType == null || use == null || instant == null) {
            return false;
        }
        return reviewState == PolicyReviewState.REVIEWED
                && surfaceGrants.containsKey(contentType)
                && isEffectiveAt(instant)
                && !prohibitedUses.contains(use);
    }

    /**
     * Returns the content types this policy speaks about, independently of which
     * surfaces each one was granted.
     *
     * @return content types covered by this policy; empty covers none
     */
    public Set<SourceContentType> contentTypes() {
        return surfaceGrants.keySet();
    }

    /**
     * Reports whether the policy is in force at an instant.
     *
     * @param instant instant to test
     * @return {@code true} when the instant falls in the effective interval
     */
    public boolean isEffectiveAt(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return !instant.isBefore(effectiveFrom)
                && (effectiveUntil == null || !instant.isAfter(effectiveUntil))
                && (revokedAt == null || instant.isBefore(revokedAt));
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
     * Reports whether the supplied source content may remain cached at an instant.
     *
     * @param mediaBytes whether the cache contains bytes rather than a link
     * @param retrievedAt instant the content was obtained
     * @param instant instant at which cache use is proposed
     * @return {@code true} only when both policy retention windows still permit the cache
     */
    public boolean allowsMediaCache(boolean mediaBytes, Instant retrievedAt, Instant instant) {
        if (retrievedAt == null || instant == null || instant.isBefore(retrievedAt)) {
            return false;
        }
        if (mediaBytes && !mediaCache.imageBytesAllowed()) {
            return false;
        }
        Duration mediaRetention = mediaBytes ? retention : mediaCache.imageLinkRetention();
        return isEffectiveAt(instant)
                && !instant.isAfter(retrievedAt.plus(retention))
                && !instant.isAfter(retrievedAt.plus(mediaRetention));
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

    /**
     * Defensively copies the per-content-type surface grants into an immutable
     * map of immutable sets, rejecting a null key or a null surface set.
     *
     * @param surfaceGrants raw grants supplied to the constructor
     * @return immutable, null-free copy; empty covers no content type
     */
    private static Map<SourceContentType, Set<ProjectionSurface>> copySurfaceGrants(
            Map<SourceContentType, Set<ProjectionSurface>> surfaceGrants) {
        if (surfaceGrants == null || surfaceGrants.isEmpty()) {
            return Map.of();
        }
        Map<SourceContentType, Set<ProjectionSurface>> copy = new EnumMap<>(SourceContentType.class);
        for (Map.Entry<SourceContentType, Set<ProjectionSurface>> entry : surfaceGrants.entrySet()) {
            SourceContentType contentType = entry.getKey();
            Set<ProjectionSurface> surfaces = entry.getValue();
            if (contentType == null) {
                throw new IllegalArgumentException("surfaceGrants must not contain a null content type");
            }
            if (surfaces == null) {
                throw new IllegalArgumentException("surfaceGrants must not contain a null surface set");
            }
            copy.put(contentType, Set.copyOf(surfaces));
        }
        return Collections.unmodifiableMap(copy);
    }
}
