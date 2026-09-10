package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SourceUsagePolicyTest {

    @Test
    void denyAllIsTheDefaultForEveryProjectionSurface() {
        SourceUsagePolicy policy = SourceUsagePolicy.denyAll(
                "icecat-unreviewed", "icecat", "1", LocalDate.parse("2026-09-09"));

        assertThat(policy.allowedSurfaces()).isEmpty();
        assertThat(ProjectionSurface.values()).allSatisfy(surface -> assertThat(policy.allows(surface)).isFalse());
        assertThat(policy.redistribution()).isEqualTo(RedistributionPolicy.PROHIBITED);
        assertThat(policy.mediaCache()).isEqualTo(MediaCachePolicy.NONE);
    }

    @Test
    void onlyExplicitSurfacesAreAllowedAndTheInputSetIsCopied() {
        Set<ProjectionSurface> surfaces = new HashSet<>(Set.of(ProjectionSurface.NUDGER_WEB));
        SourceUsagePolicy policy = new SourceUsagePolicy(
                "eprel-public-api",
                "eprel",
                "2024-06-03",
                surfaces,
                Duration.ofDays(30),
                MediaCachePolicy.NONE,
                new AttributionRequirement(
                        true,
                        "European Commission EPREL",
                        URI.create("https://eprel.ec.europa.eu")),
                RedistributionPolicy.VALUE_ADDED_ONLY,
                LocalDate.parse("2026-09-09"));

        surfaces.add(ProjectionSurface.B2B_API);

        assertThat(policy.allows(ProjectionSurface.NUDGER_WEB)).isTrue();
        assertThat(policy.allows(ProjectionSurface.B2B_API)).isFalse();
        assertThat(policy.allows(ProjectionSurface.ODBL_EXPORT)).isFalse();
        assertThat(policy.reference()).isEqualTo(new SourceUsagePolicyRef("eprel-public-api", "2024-06-03"));
    }

    @Test
    void requiredAttributionCannotBeIncomplete() {
        assertThatThrownBy(() -> new AttributionRequirement(true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
