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
    private static final Instant DURING = Instant.parse("2026-09-30T00:00:00Z");

    @Test
    void defaultInventoryHoldsTheElevenGou95RatifiedRowsAllReviewedWithEvidence() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();

        // GOU-95/GOU-105: the generic merchant-feed row is replaced by six per-network rows,
        // because a single record cannot carry six distinct publisher agreements.
        assertThat(registry.policies()).extracting(SourceUsagePolicy::sourceId)
                .containsExactly(new SourceId("eprel"), new SourceId("icecat"), new SourceId("icecat.full"),
                        new SourceId("merchant-feed.awin"), new SourceId("merchant-feed.effiliation"),
                        new SourceId("merchant-feed.tradetracker"), new SourceId("merchant-feed.kwanko"),
                        new SourceId("merchant-feed.webgains"), new SourceId("merchant-feed.cj"),
                        new SourceId("legacy-product-backup"), new SourceId("amazon-paapi"));

        for (SourceUsagePolicy policy : registry.policies()) {
            assertThat(policy.evidenceReferences()).as("%s evidence", policy.sourceId()).isNotEmpty();
            // A row in total refusal is still a decided, reviewed refusal, not an unreviewed default.
            assertThat(policy.reviewState()).as("%s reviewState", policy.sourceId())
                    .isEqualTo(PolicyReviewState.REVIEWED);
            assertThat(policy.legalReviewDate()).as("%s legalReviewDate", policy.sourceId())
                    .isEqualTo(LocalDate.of(2026, 9, 29));
        }
    }

    @Test
    void odblExportIsNotGrantedByAnyOfTheElevenRatifiedRows() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();

        for (SourceUsagePolicy policy : registry.policies()) {
            for (SourceContentType contentType : SourceContentType.values()) {
                assertThat(registry.allows(policy.sourceId(), policy.reference(), contentType,
                        ProjectionSurface.ODBL_EXPORT, DURING))
                        .as("%s allows %s on ODBL_EXPORT", policy.sourceId(), contentType)
                        .isFalse();
            }
        }
    }

    @Test
    void eprelIsAllowedOnWebAndB2bForItsThreeEmittedContentTypesButNotBeyond() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();
        SourceId eprel = new SourceId("eprel");
        SourceUsagePolicyRef ref = registry.find(new SourceUsagePolicyRef("eprel-public-api", "2")).orElseThrow()
                .reference();

        assertThat(registry.allows(eprel, ref, SourceContentType.ATTRIBUTE, ProjectionSurface.NUDGER_WEB, DURING))
                .isTrue();
        assertThat(registry.allows(eprel, ref, SourceContentType.ATTRIBUTE, ProjectionSurface.B2B_API, DURING))
                .isTrue();
        // TEXT is never emitted by the EPREL adapter, so it was never reviewed.
        assertThat(registry.allows(eprel, ref, SourceContentType.TEXT, ProjectionSurface.NUDGER_WEB, DURING))
                .isFalse();
    }

    @Test
    void icecatOpenContentIsAllowedOnWebOnlyAndExcludesSyntheticContentGenerationNotAiTraining() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();
        SourceId icecat = new SourceId("icecat");
        SourceUsagePolicyRef ref = new SourceUsagePolicyRef("icecat-open-content", "2");

        assertThat(registry.allows(icecat, ref, SourceContentType.TEXT, ProjectionSurface.NUDGER_WEB, DURING))
                .isTrue();
        // OPL forbids charging for network access to the content: B2B_API stays refused.
        assertThat(registry.allows(icecat, ref, SourceContentType.TEXT, ProjectionSurface.B2B_API, DURING))
                .isFalse();
        // OFFER/PRICE are not Icecat content types.
        assertThat(registry.allows(icecat, ref, SourceContentType.OFFER, ProjectionSurface.NUDGER_WEB, DURING))
                .isFalse();

        // GOU-95 answer 4: only synthetic content generation is excluded; inference embeddings remain allowed.
        assertThat(registry.allowsUse(icecat, ref, SourceContentType.TEXT, ProhibitedUse.AI_TRAINING, DURING))
                .isTrue();
        assertThat(registry.allowsUse(icecat, ref, SourceContentType.TEXT, ProhibitedUse.SYNTHETIC_CONTENT_GENERATION,
                DURING)).isFalse();
    }

    @Test
    void icecatFullSubscriptionRemainsATotalRefusal() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();
        SourceId icecatFull = new SourceId("icecat.full");
        SourceUsagePolicyRef ref = new SourceUsagePolicyRef("icecat-full-subscription", "1");

        assertThat(registry.allows(icecatFull, ref, SourceContentType.IDENTITY, ProjectionSurface.NUDGER_WEB, DURING))
                .isFalse();
        assertThat(registry.allowsUse(icecatFull, ref, SourceContentType.IDENTITY, ProhibitedUse.AI_TRAINING, DURING))
                .isFalse();
    }

    @Test
    void awinAndEffiliationAllowOfferAndPriceOnWebAndB2bButNotTextOrMedia() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();

        for (String network : List.of("awin", "effiliation")) {
            SourceId source = new SourceId("merchant-feed." + network);
            SourceUsagePolicyRef ref = new SourceUsagePolicyRef("merchant-feed." + network, "1");

            assertThat(registry.allows(source, ref, SourceContentType.OFFER, ProjectionSurface.NUDGER_WEB, DURING))
                    .as("%s OFFER/NUDGER_WEB", network).isTrue();
            assertThat(registry.allows(source, ref, SourceContentType.PRICE, ProjectionSurface.B2B_API, DURING))
                    .as("%s PRICE/B2B_API", network).isTrue();
            // The publisher agreement covers OFFER/PRICE, not the merchant's own text or images.
            assertThat(registry.allows(source, ref, SourceContentType.TEXT, ProjectionSurface.NUDGER_WEB, DURING))
                    .as("%s TEXT/NUDGER_WEB", network).isFalse();
            assertThat(registry.allows(source, ref, SourceContentType.MEDIA, ProjectionSurface.NUDGER_WEB, DURING))
                    .as("%s MEDIA/NUDGER_WEB", network).isFalse();
        }
    }

    @Test
    void theFourNetworksWithoutAPublisherAgreementRemainTotalRefusals() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();

        for (String network : List.of("tradetracker", "kwanko", "webgains", "cj")) {
            SourceId source = new SourceId("merchant-feed." + network);
            SourceUsagePolicyRef ref = new SourceUsagePolicyRef("merchant-feed." + network, "1");

            assertThat(registry.allows(source, ref, SourceContentType.OFFER, ProjectionSurface.NUDGER_WEB, DURING))
                    .as("%s OFFER/NUDGER_WEB", network).isFalse();
            assertThat(registry.allows(source, ref, SourceContentType.PRICE, ProjectionSurface.B2B_API, DURING))
                    .as("%s PRICE/B2B_API", network).isFalse();
        }
    }

    @Test
    void legacyProductBackupPublishesIdentityOnNudgerWebOnlyUnderValueAddedOnlyRedistribution()
            throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();
        SourceId source = new SourceId("legacy-product-backup");
        SourceUsagePolicyRef ref = new SourceUsagePolicyRef("legacy-product-backup", "3");

        // Lead Tech/Goulven resolved the GOU-105 coherence question on the ask_user_questions
        // interaction: redistribution moves to VALUE_ADDED_ONLY so the ratified IDENTITY/NUDGER_WEB
        // surfaceGrant is actually effective, instead of being shadowed by a PROHIBITED gate.
        assertThat(registry.allows(source, ref, SourceContentType.IDENTITY, ProjectionSurface.NUDGER_WEB, DURING))
                .isTrue();
        assertThat(registry.allows(source, ref, SourceContentType.IDENTITY, ProjectionSurface.B2B_API, DURING))
                .isFalse();
        assertThat(registry.find(ref).orElseThrow().contentTypes()).containsExactly(SourceContentType.IDENTITY);
        assertThat(registry.find(ref).orElseThrow().attribution().required()).isFalse();
    }

    @Test
    void amazonQuarantineIsRevokedAndDeniesAnyAssertionFromItsRevocationOnward() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();
        SourceUsagePolicy amazon = registry.find(new SourceUsagePolicyRef("amazon-paapi-quarantine", "2"))
                .orElseThrow();
        Instant revokedAt = Instant.parse("2026-09-29T00:00:00Z");

        assertThat(amazon.revokedAt()).isEqualTo(revokedAt);
        assertThat(amazon.isEffectiveAt(revokedAt.minusSeconds(1))).isTrue();
        assertThat(amazon.isEffectiveAt(revokedAt)).isFalse();
        assertThat(amazon.isEffectiveAt(Instant.parse("2030-01-01T00:00:00Z"))).isFalse();
        assertThat(registry.allows(amazon.sourceId(), amazon.reference(), SourceContentType.IDENTITY,
                ProjectionSurface.NUDGER_WEB, revokedAt.minusSeconds(1))).isFalse();
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
    void allowsUseDeniesEveryUseForEveryTotalRefusalRowInTheDefaultInventory() throws IOException {
        SourceUsagePolicyRegistry registry = SourceUsagePolicyRegistry.loadDefault();
        // eprel and icecat are the two rows with a reviewed named-use clearance
        // (eprel clears every use with an empty prohibitedUses; icecat clears AI_TRAINING);
        // every other row is a total, reviewed refusal covering every use.
        Set<SourceId> reviewedForSomeUse = Set.of(new SourceId("eprel"), new SourceId("icecat"));

        for (SourceUsagePolicy policy : registry.policies()) {
            if (reviewedForSomeUse.contains(policy.sourceId())) {
                continue;
            }
            for (SourceContentType contentType : SourceContentType.values()) {
                for (ProhibitedUse use : ProhibitedUse.values()) {
                    assertThat(registry.allowsUse(policy.sourceId(), policy.reference(), contentType, use, DURING))
                            .as("%s allowsUse %s on %s", policy.sourceId(), use, contentType)
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
