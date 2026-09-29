package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Usage policy denies unless the source, content type, surface and instant were
 * all explicitly approved.
 */
class SourceUsagePolicyTest {

    private static final SourceId SOURCE = new SourceId("icecat");
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UNTIL = Instant.parse("2026-12-31T23:59:59Z");
    private static final Instant DURING = Instant.parse("2026-06-01T00:00:00Z");

    private static SourceUsagePolicy policy(SourceContentType type, ProjectionSurface surface) {
        return policy(Map.of(type, Set.of(surface)));
    }

    private static SourceUsagePolicy policy(Map<SourceContentType, Set<ProjectionSurface>> surfaceGrants) {
        return new SourceUsagePolicy("icecat-standard", SOURCE, "3", surfaceGrants, FROM, UNTIL,
                Duration.ofDays(365), MediaCachePolicy.NONE,
                new AttributionRequirement(true, "Data by Icecat", URI.create("https://icecat.biz"), true),
                RedistributionPolicy.ALLOWED, DerivativeLicence.SHARE_ALIKE, Set.of(),
                LocalDate.of(2026, 1, 1), List.of(URI.create("https://icecat.biz/terms")));
    }

    @Test
    void denyAllPermitsNothing() {
        SourceUsagePolicy denied = SourceUsagePolicy.denyAll("none", SOURCE, "1", FROM, LocalDate.of(2026, 1, 1));

        for (SourceContentType contentType : SourceContentType.values()) {
            for (ProjectionSurface surface : ProjectionSurface.values()) {
                assertThat(denied.allows(contentType, surface, DURING)).isFalse();
            }
        }
    }

    @Test
    void paapiQuarantinePermitsNoPublicSurface() {
        for (SourceContentType contentType : SourceContentType.values()) {
            for (ProjectionSurface surface : ProjectionSurface.values()) {
                assertThat(PaapiQuarantinePolicy.POLICY.allows(contentType, surface, DURING)).isFalse();
            }
        }
    }

    @Test
    void permitsOnlyTheApprovedContentTypeAndSurface() {
        SourceUsagePolicy classificationOnWebOnly =
                policy(SourceContentType.CLASSIFICATION, ProjectionSurface.NUDGER_WEB);

        assertThat(classificationOnWebOnly.allows(
                SourceContentType.CLASSIFICATION, ProjectionSurface.NUDGER_WEB, DURING)).isTrue();
        // Same surface, a content type nobody approved.
        assertThat(classificationOnWebOnly.allows(
                SourceContentType.TEXT, ProjectionSurface.NUDGER_WEB, DURING)).isFalse();
        // Same content type, a surface nobody approved.
        assertThat(classificationOnWebOnly.allows(
                SourceContentType.CLASSIFICATION, ProjectionSurface.ODBL_EXPORT, DURING)).isFalse();
    }

    @Test
    void grantingOneContentTypeOnASurfaceDoesNotGrantOthersOnTheSameSurface() {
        SourceUsagePolicy identityOnlyOnOdblExport = policy(Map.of(
                SourceContentType.IDENTITY, Set.of(ProjectionSurface.ODBL_EXPORT),
                SourceContentType.ATTRIBUTE, Set.of(),
                SourceContentType.TEXT, Set.of(),
                SourceContentType.MEDIA, Set.of()));

        assertThat(identityOnlyOnOdblExport.allows(
                SourceContentType.IDENTITY, ProjectionSurface.ODBL_EXPORT, DURING)).isTrue();
        assertThat(identityOnlyOnOdblExport.allows(
                SourceContentType.ATTRIBUTE, ProjectionSurface.ODBL_EXPORT, DURING)).isFalse();
        assertThat(identityOnlyOnOdblExport.allows(
                SourceContentType.TEXT, ProjectionSurface.ODBL_EXPORT, DURING)).isFalse();
        assertThat(identityOnlyOnOdblExport.allows(
                SourceContentType.MEDIA, ProjectionSurface.ODBL_EXPORT, DURING)).isFalse();
        // The content types with no granted surface are still reviewed, for allowsUse.
        assertThat(identityOnlyOnOdblExport.allowsUse(
                SourceContentType.ATTRIBUTE, ProhibitedUse.AI_TRAINING, DURING)).isTrue();
    }

    @Test
    void permitsNothingOutsideTheEffectiveInterval() {
        SourceUsagePolicy permitted =
                policy(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API);

        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API,
                Instant.parse("2025-12-31T23:59:59Z"))).isFalse();
        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API, DURING)).isTrue();
        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API,
                Instant.parse("2027-01-01T00:00:00Z"))).isFalse();
    }

    @Test
    void anOpenEndedPolicyStaysInForce() {
        SourceUsagePolicy openEnded = new SourceUsagePolicy("p", SOURCE, "1",
                Map.of(SourceContentType.IDENTITY, Set.of(ProjectionSurface.NUDGER_WEB)), FROM, null,
                Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE,
                RedistributionPolicy.ALLOWED, DerivativeLicence.NONE, Set.of(),
                LocalDate.of(2026, 1, 1), List.of(URI.create("https://example.test/terms")));

        assertThat(openEnded.isEffectiveAt(Instant.parse("2099-01-01T00:00:00Z"))).isTrue();
    }

    @Test
    void treatsNullArgumentsAsDenied() {
        SourceUsagePolicy permitted =
                policy(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API);

        assertThat(permitted.allows(null, ProjectionSurface.B2B_API, DURING)).isFalse();
        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, null, DURING)).isFalse();
        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API, null)).isFalse();
    }

    @Test
    void rejectsAnIntervalThatEndsBeforeItBegins() {
        assertThatThrownBy(() -> new SourceUsagePolicy("p", SOURCE, "1", Map.of(), UNTIL, FROM,
                Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE,
                RedistributionPolicy.PROHIBITED, DerivativeLicence.NONE, Set.of(),
                LocalDate.of(2026, 1, 1), List.of(URI.create("https://example.test/terms"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveUntil must not precede effectiveFrom");
    }

    @Test
    void prohibitsPublicationWhenRedistributionIsNotPermitted() {
        SourceUsagePolicy prohibited = new SourceUsagePolicy("p", SOURCE, "1",
                Map.of(SourceContentType.ATTRIBUTE, Set.of(ProjectionSurface.NUDGER_WEB)), FROM, null,
                Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE,
                RedistributionPolicy.PROHIBITED, DerivativeLicence.NONE, Set.of(), LocalDate.of(2026, 1, 1),
                List.of(URI.create("https://example.test/terms")));

        assertThat(prohibited.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.NUDGER_WEB, DURING)).isFalse();
    }

    @Test
    void allowsUseOnlyForTheReviewedContentTypeWithinTheEffectiveIntervalAndWhenNotProhibited() {
        SourceUsagePolicy cleared = policy(SourceContentType.CLASSIFICATION, ProjectionSurface.NUDGER_WEB);

        assertThat(cleared.allowsUse(SourceContentType.CLASSIFICATION, ProhibitedUse.AI_TRAINING, DURING)).isTrue();
        // A content type nobody reviewed for this use.
        assertThat(cleared.allowsUse(SourceContentType.TEXT, ProhibitedUse.AI_TRAINING, DURING)).isFalse();
        // Outside the effective interval.
        assertThat(cleared.allowsUse(SourceContentType.CLASSIFICATION, ProhibitedUse.AI_TRAINING,
                Instant.parse("2027-01-01T00:00:00Z"))).isFalse();
    }

    @Test
    void allowsUseDeniesAnUseTheSourceProhibits() {
        SourceUsagePolicy trainingProhibited = new SourceUsagePolicy("p", SOURCE, "1",
                Map.of(SourceContentType.TEXT, Set.of(ProjectionSurface.NUDGER_WEB)), FROM, UNTIL,
                Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE, RedistributionPolicy.PROHIBITED,
                DerivativeLicence.SHARE_ALIKE, Set.of(ProhibitedUse.AI_TRAINING), LocalDate.of(2026, 1, 1),
                List.of(URI.create("https://example.test/terms")));

        assertThat(trainingProhibited.allowsUse(SourceContentType.TEXT, ProhibitedUse.AI_TRAINING, DURING)).isFalse();
        assertThat(trainingProhibited.allowsUse(SourceContentType.TEXT, ProhibitedUse.SYNTHETIC_CONTENT_GENERATION, DURING))
                .isTrue();
    }

    @Test
    void allowsUseReturnsFalseForAnUnreviewedPolicyExactlyAsAllowsDoes() {
        SourceUsagePolicy denied = SourceUsagePolicy.denyAll("none", SOURCE, "1", FROM, LocalDate.of(2026, 1, 1));

        for (SourceContentType contentType : SourceContentType.values()) {
            for (ProhibitedUse use : ProhibitedUse.values()) {
                assertThat(denied.allowsUse(contentType, use, DURING)).isFalse();
            }
        }
    }

    @Test
    void allowsUseDeniesAnUnreviewedPolicyEvenWithClearedContentAndUses() {
        SourceUsagePolicy unreviewed = new SourceUsagePolicy("p", SOURCE, "1",
                Map.of(SourceContentType.ATTRIBUTE, Set.of(ProjectionSurface.NUDGER_WEB)), FROM, UNTIL,
                Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE, RedistributionPolicy.ALLOWED,
                DerivativeLicence.NONE, Set.of(), LocalDate.of(2026, 1, 1), PolicyReviewState.UNREVIEWED, null,
                List.of(URI.create("https://example.test/terms")));

        assertThat(unreviewed.allowsUse(SourceContentType.ATTRIBUTE, ProhibitedUse.AI_TRAINING, DURING)).isFalse();
    }

    @Test
    void allowsUseTreatsNullArgumentsAsDenied() {
        SourceUsagePolicy cleared = policy(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API);

        assertThat(cleared.allowsUse(null, ProhibitedUse.AI_TRAINING, DURING)).isFalse();
        assertThat(cleared.allowsUse(SourceContentType.ATTRIBUTE, null, DURING)).isFalse();
        assertThat(cleared.allowsUse(SourceContentType.ATTRIBUTE, ProhibitedUse.AI_TRAINING, null)).isFalse();
    }

    @Test
    void anAbsentDerivativeLicenceAndProhibitedUsesReadAsTheMostRestrictive() {
        SourceUsagePolicy undeclared = new SourceUsagePolicy("p", SOURCE, "1",
                Map.of(SourceContentType.ATTRIBUTE, Set.of(ProjectionSurface.NUDGER_WEB)), FROM, UNTIL,
                Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE, RedistributionPolicy.ALLOWED,
                null, null, LocalDate.of(2026, 1, 1), PolicyReviewState.REVIEWED, null,
                List.of(URI.create("https://example.test/terms")));

        assertThat(undeclared.derivativeLicence()).isEqualTo(DerivativeLicence.NONE);
        assertThat(undeclared.prohibitedUses()).containsExactlyInAnyOrder(ProhibitedUse.values());
        for (ProhibitedUse use : ProhibitedUse.values()) {
            assertThat(undeclared.allowsUse(SourceContentType.ATTRIBUTE, use, DURING)).isFalse();
        }
    }

    @Test
    void denyAllProhibitsEveryUseAndCarriesNoDerivativeLicence() {
        SourceUsagePolicy denied = SourceUsagePolicy.denyAll("none", SOURCE, "1", FROM, LocalDate.of(2026, 1, 1));

        assertThat(denied.derivativeLicence()).isEqualTo(DerivativeLicence.NONE);
        assertThat(denied.prohibitedUses()).containsExactlyInAnyOrder(ProhibitedUse.values());
    }
}
