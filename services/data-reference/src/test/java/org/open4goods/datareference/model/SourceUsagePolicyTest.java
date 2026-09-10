package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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

    private static SourceUsagePolicy policy(Set<SourceContentType> types, Set<ProjectionSurface> surfaces) {
        return new SourceUsagePolicy("icecat-standard", SOURCE, "3", types, surfaces, FROM, UNTIL,
                Duration.ofDays(365), MediaCachePolicy.NONE,
                new AttributionRequirement(true, "Data by Icecat", URI.create("https://icecat.biz")),
                RedistributionPolicy.PROHIBITED, LocalDate.of(2026, 1, 1));
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
    void permitsOnlyTheApprovedContentTypeAndSurface() {
        SourceUsagePolicy classificationOnWebOnly =
                policy(Set.of(SourceContentType.CLASSIFICATION), Set.of(ProjectionSurface.NUDGER_WEB));

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
    void permitsNothingOutsideTheEffectiveInterval() {
        SourceUsagePolicy permitted =
                policy(Set.of(SourceContentType.ATTRIBUTE), Set.of(ProjectionSurface.B2B_API));

        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API,
                Instant.parse("2025-12-31T23:59:59Z"))).isFalse();
        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API, DURING)).isTrue();
        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API,
                Instant.parse("2027-01-01T00:00:00Z"))).isFalse();
    }

    @Test
    void anOpenEndedPolicyStaysInForce() {
        SourceUsagePolicy openEnded = new SourceUsagePolicy("p", SOURCE, "1",
                Set.of(SourceContentType.IDENTITY), Set.of(ProjectionSurface.NUDGER_WEB), FROM, null,
                Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE,
                RedistributionPolicy.PROHIBITED, LocalDate.of(2026, 1, 1));

        assertThat(openEnded.isEffectiveAt(Instant.parse("2099-01-01T00:00:00Z"))).isTrue();
    }

    @Test
    void treatsNullArgumentsAsDenied() {
        SourceUsagePolicy permitted =
                policy(Set.of(SourceContentType.ATTRIBUTE), Set.of(ProjectionSurface.B2B_API));

        assertThat(permitted.allows(null, ProjectionSurface.B2B_API, DURING)).isFalse();
        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, null, DURING)).isFalse();
        assertThat(permitted.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API, null)).isFalse();
    }

    @Test
    void rejectsAnIntervalThatEndsBeforeItBegins() {
        assertThatThrownBy(() -> new SourceUsagePolicy("p", SOURCE, "1", Set.of(), Set.of(), UNTIL, FROM,
                Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE,
                RedistributionPolicy.PROHIBITED, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveUntil must not precede effectiveFrom");
    }
}
