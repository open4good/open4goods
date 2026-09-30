package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.serialization.DataReferenceJson;

/**
 * Policy inventory and publication-gate boundary tests.
 */
class SourceUsagePolicyRegistryTest {

    private static final SourceId FIXTURE_SOURCE = new SourceId("fixture-source");
    private static final SourceUsagePolicyRef FIXTURE_POLICY = new SourceUsagePolicyRef("fixture-reviewed", "1");
    private static final Instant DURING = Instant.parse("2026-06-01T00:00:00Z");

    @Test
    void defaultInventoryRecordsEveryKnownSourceAsAnUnreviewedDeny() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();

        assertThat(registry.policies()).extracting(SourceUsagePolicy::sourceId)
                .containsExactly(new SourceId("eprel"), new SourceId("icecat"), new SourceId("merchant-feed"),
                        new SourceId("legacy-product-backup"), new SourceId("amazon-paapi"));
        for (SourceUsagePolicy policy : registry.policies()) {
            assertThat(policy.reviewState()).isEqualTo(PolicyReviewState.UNREVIEWED);
            assertThat(policy.evidenceReferences()).isNotEmpty();
            for (SourceContentType contentType : policy.contentTypes()) {
                for (ProjectionSurface surface : ProjectionSurface.values()) {
                    assertThat(registry.allows(policy.sourceId(), policy.reference(), contentType, surface, DURING))
                            .isFalse();
                }
            }
        }
    }

    @Test
    void absentAndSourceMismatchedPoliciesDenyPublication() throws IOException {
        SourceUsagePolicyRegistry registry = fixtureRegistry();

        assertThat(registry.allows(FIXTURE_SOURCE, new SourceUsagePolicyRef("missing", "1"),
                SourceContentType.ATTRIBUTE, ProjectionSurface.NUDGER_WEB, DURING)).isFalse();
        assertThat(registry.allows(new SourceId("other-source"), FIXTURE_POLICY,
                SourceContentType.ATTRIBUTE, ProjectionSurface.NUDGER_WEB, DURING)).isFalse();
    }

    @Test
    void reviewedFixturePermitsOnlyItsContentAcrossEachApprovedSurfaceAndPeriod() throws IOException {
        SourceUsagePolicyRegistry registry = fixtureRegistry();

        for (ProjectionSurface surface : ProjectionSurface.values()) {
            assertThat(registry.allows(FIXTURE_SOURCE, FIXTURE_POLICY, SourceContentType.ATTRIBUTE, surface,
                    Instant.parse("2026-01-01T00:00:00Z"))).isTrue();
            assertThat(registry.allows(FIXTURE_SOURCE, FIXTURE_POLICY, SourceContentType.ATTRIBUTE, surface,
                    Instant.parse("2026-12-31T23:59:59Z"))).isTrue();
            assertThat(registry.allows(FIXTURE_SOURCE, FIXTURE_POLICY, SourceContentType.ATTRIBUTE, surface,
                    Instant.parse("2027-01-01T00:00:00Z"))).isFalse();
            assertThat(registry.allows(FIXTURE_SOURCE, FIXTURE_POLICY, SourceContentType.TEXT, surface, DURING))
                    .isFalse();
        }
    }

    @Test
    void revocationTakesEffectAtItsRecordedInstant() {
        SourceUsagePolicy revoked = new SourceUsagePolicy("revoked", FIXTURE_SOURCE, "1",
                Map.of(SourceContentType.ATTRIBUTE, Set.of(ProjectionSurface.NUDGER_WEB)),
                Instant.parse("2026-01-01T00:00:00Z"), null, Duration.ofDays(30), MediaCachePolicy.NONE,
                AttributionRequirement.NONE, RedistributionPolicy.ALLOWED, DerivativeLicence.NONE, Set.of(),
                LocalDate.of(2026, 1, 1), PolicyReviewState.REVIEWED, Instant.parse("2026-06-01T00:00:00Z"),
                List.of(URI.create("https://example.test/terms")));

        assertThat(revoked.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.NUDGER_WEB,
                Instant.parse("2026-05-31T23:59:59Z"))).isTrue();
        assertThat(revoked.allows(SourceContentType.ATTRIBUTE, ProjectionSurface.NUDGER_WEB, DURING)).isFalse();
    }

    @Test
    void mediaNeedsTheApprovedSurfaceAndRespectsCacheAndAttributionBoundaries() throws IOException {
        SourceUsagePolicyRegistry registry = fixtureRegistry();
        Instant retrievedAt = Instant.parse("2026-06-01T00:00:00Z");

        assertThat(registry.find(FIXTURE_POLICY).orElseThrow().attribution().required()).isTrue();
        assertThat(registry.allowsMediaCache(FIXTURE_SOURCE, FIXTURE_POLICY, ProjectionSurface.NUDGER_WEB,
                false, retrievedAt, retrievedAt.plus(Duration.ofDays(7)))).isTrue();
        assertThat(registry.allowsMediaCache(FIXTURE_SOURCE, FIXTURE_POLICY, ProjectionSurface.NUDGER_WEB,
                false, retrievedAt, retrievedAt.plus(Duration.ofDays(7)).plusSeconds(1))).isFalse();
        assertThat(registry.allowsMediaCache(FIXTURE_SOURCE, FIXTURE_POLICY, ProjectionSurface.NUDGER_WEB,
                true, retrievedAt, retrievedAt.plus(Duration.ofHours(1)))).isFalse();
        assertThat(registry.allowsMediaCache(FIXTURE_SOURCE, FIXTURE_POLICY, ProjectionSurface.NUDGER_WEB,
                false, retrievedAt, Instant.parse("2027-01-01T00:00:00Z"))).isFalse();
    }

    @Test
    void loadsAFixtureThatPredatesTheNewFieldsWithTheMostRestrictiveDefaults() throws IOException {
        SourceUsagePolicy fixture = fixtureRegistry().find(FIXTURE_POLICY).orElseThrow();

        assertThat(fixture.derivativeLicence()).isEqualTo(DerivativeLicence.NONE);
        assertThat(fixture.prohibitedUses()).containsExactlyInAnyOrder(ProhibitedUse.values());
        assertThat(fixture.attribution().asIsDisclaimerRequired()).isTrue();
    }

    @Test
    void allowsUseMirrorsAllowsForAnExplicitlyClearedUse() {
        SourceUsagePolicyRef reference = new SourceUsagePolicyRef("cleared-use", "1");
        SourceUsagePolicy cleared = new SourceUsagePolicy("cleared-use", FIXTURE_SOURCE, "1",
                Map.of(SourceContentType.TEXT, Set.of(ProjectionSurface.NUDGER_WEB)),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T23:59:59Z"), Duration.ZERO,
                MediaCachePolicy.NONE, AttributionRequirement.NONE, RedistributionPolicy.PROHIBITED,
                DerivativeLicence.NONE, Set.of(), LocalDate.of(2026, 1, 1),
                List.of(URI.create("https://example.test/terms")));
        SourceUsagePolicyRegistry registry = new SourceUsagePolicyRegistry(
                new SourceUsagePolicyDocument(SourceUsagePolicyDocument.SCHEMA_VERSION, List.of(cleared)));

        assertThat(registry.allowsUse(FIXTURE_SOURCE, reference, SourceContentType.TEXT, ProhibitedUse.AI_TRAINING, DURING))
                .isTrue();
        // Content type nobody reviewed for this use.
        assertThat(registry.allowsUse(FIXTURE_SOURCE, reference, SourceContentType.MEDIA, ProhibitedUse.AI_TRAINING, DURING))
                .isFalse();
        // Source mismatched against the policy's own source.
        assertThat(registry.allowsUse(new SourceId("other-source"), reference, SourceContentType.TEXT,
                ProhibitedUse.AI_TRAINING, DURING)).isFalse();
        // Reference absent from the registry.
        assertThat(registry.allowsUse(FIXTURE_SOURCE, new SourceUsagePolicyRef("missing", "1"),
                SourceContentType.TEXT, ProhibitedUse.AI_TRAINING, DURING)).isFalse();
    }

    @Test
    void allowsUseDeniesEveryUseForTheUnreviewedDefaultInventory() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();

        for (SourceUsagePolicy policy : registry.policies()) {
            for (SourceContentType contentType : SourceContentType.values()) {
                for (ProhibitedUse use : ProhibitedUse.values()) {
                    assertThat(registry.allowsUse(policy.sourceId(), policy.reference(), contentType, use, DURING))
                            .isFalse();
                }
            }
        }
    }

    @Test
    void derivedFieldsCannotInheritProviderPermission() throws IOException {
        assertThat(fixtureRegistry().allowsDerivedField()).isFalse();
    }

    @Test
    void rejectsDuplicatePolicyVersions() {
        SourceUsagePolicy policy = new SourceUsagePolicy("p", FIXTURE_SOURCE, "1", Map.of(),
                Instant.EPOCH, null, Duration.ZERO, MediaCachePolicy.NONE, AttributionRequirement.NONE,
                RedistributionPolicy.PROHIBITED, DerivativeLicence.NONE, Set.of(), LocalDate.of(2026, 1, 1),
                List.of(URI.create("https://example.test/terms")));

        assertThatThrownBy(() -> new SourceUsagePolicyRegistry(new SourceUsagePolicyDocument(
                SourceUsagePolicyDocument.SCHEMA_VERSION, List.of(policy, policy))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate source usage policy");
    }

    private static SourceUsagePolicyRegistry fixtureRegistry() throws IOException {
        try (var stream = SourceUsagePolicyRegistryTest.class
                .getResourceAsStream("/policy/reviewed-source-usage-policy.json")) {
            if (stream == null) {
                throw new IllegalStateException("source usage policy fixture is missing");
            }
            return new SourceUsagePolicyRegistry(DataReferenceJson.mapper().readValue(
                    new String(stream.readAllBytes(), StandardCharsets.UTF_8), SourceUsagePolicyDocument.class));
        }
    }
}
