package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.AttributionRequirement;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.GtinMatchMethod;
import org.open4goods.datareference.model.MediaCachePolicy;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.PolicyReviewState;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RedistributionPolicy;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicy;
import org.open4goods.datareference.model.SourceUsagePolicyDocument;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.normalization.NormalizationResult;
import org.open4goods.datareference.model.normalization.NormalizationStatus;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.Correction;
import org.open4goods.datareference.model.resolution.NormalizedValue;
import org.open4goods.datareference.model.resolution.ResolutionReason;
import org.open4goods.datareference.model.resolution.ResolutionRule;
import org.open4goods.datareference.model.resolution.ResolutionRuleRegistry;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.port.CorrectionsPort;
import org.open4goods.datareference.port.ScanPage;

class DeterministicResolutionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");
    private static final Gtin GTIN = new Gtin("12345670");
    private static final CanonicalAttributeId ATTRIBUTE = new CanonicalAttributeId("energy-class");

    @Test
    void resolvesTheConfiguredAuthorityIndependentlyOfInputOrderAndRejectsDeniedEvidence() {
        SourceRecordHead merchant = head("merchant", "merchant-1", "B", GtinMatchConfidence.EXACT);
        SourceRecordHead regulator = head("regulator", "regulator-1", "A", GtinMatchConfidence.WEAK);
        SourceRecordHead denied = head("denied", "denied-1", "Z", GtinMatchConfidence.EXACT);
        SourceRecordHead unverified = head("merchant", "merchant-2", "C", GtinMatchConfidence.UNVERIFIED);
        DeterministicResolutionService service = service();

        var first = service.resolve(GTIN, List.of(merchant, denied, regulator, unverified), ProjectionSurface.NUDGER_WEB, NOW);
        var second = service.resolve(GTIN, List.of(regulator, unverified, merchant, denied), ProjectionSurface.NUDGER_WEB, NOW);

        assertThat(first).hasSize(1);
        assertThat(first).isEqualTo(second);
        assertThat(first.getFirst().value()).isEqualTo(new CodeValue("energy", "A"));
        assertThat(first.getFirst().reason()).isEqualTo(ResolutionReason.SOURCE_AUTHORITY);
        assertThat(first.getFirst().candidateAssertionIds()).hasSize(2);
        assertThat(first.getFirst().conflicting()).isTrue();
    }

    @Test
    void revokedCorrectionRestoresThePriorDeterministicWinnerOnReplay() {
        SourceRecordHead merchant = head("merchant", "merchant-1", "B", GtinMatchConfidence.EXACT);
        SourceRecordHead regulator = head("regulator", "regulator-1", "A", GtinMatchConfidence.WEAK);
        Instant revokedAt = NOW.plusSeconds(30);
        Correction correction = new Correction(new org.open4goods.datareference.model.AssertionId("o4g:energy-class:review-1"),
                GTIN, ATTRIBUTE, new CodeValue("energy", "C"), NOW.minusSeconds(30), "reviewer@example.test",
                "The regulator catalogue temporarily published a known wrong value.", Set.of(ProjectionSurface.NUDGER_WEB), revokedAt);
        DeterministicResolutionService service = service(corrections(correction));

        var corrected = service.resolve(GTIN, List.of(merchant, regulator), ProjectionSurface.NUDGER_WEB, NOW);
        var restored = service.resolve(GTIN, List.of(regulator, merchant), ProjectionSurface.NUDGER_WEB, revokedAt);

        assertThat(corrected.getFirst().value()).isEqualTo(new CodeValue("energy", "C"));
        assertThat(corrected.getFirst().reason()).isEqualTo(ResolutionReason.O4G_CORRECTION);
        assertThat(restored.getFirst().value()).isEqualTo(new CodeValue("energy", "A"));
        assertThat(restored.getFirst().reason()).isEqualTo(ResolutionReason.SOURCE_AUTHORITY);
    }

    private static DeterministicResolutionService service() {
        return service(noCorrections());
    }

    private static DeterministicResolutionService service(CorrectionsPort corrections) {
        SourceUsagePolicyRegistry policies = new SourceUsagePolicyRegistry(new SourceUsagePolicyDocument(
                SourceUsagePolicyDocument.SCHEMA_VERSION, List.of(policy("merchant", true), policy("regulator", true),
                        policy("denied", false))));
        ResolutionRule rule = new ResolutionRule(ATTRIBUTE, ProjectionSurface.NUDGER_WEB,
                new RuleVersion("energy-class-resolution", 1), List.of(new SourceId("regulator"), new SourceId("merchant")),
                new SourceId("regulator"));
        return new DeterministicResolutionService(policies, request -> {
            String code = ((ScalarEvidence) request.assertion().evidence()).lexicalValue();
            return new NormalizationResult(request.assertion().assertionId(), new RegistryVersion(1),
                    org.open4goods.datareference.model.LanguageTag.UND, NormalizationStatus.SUCCESS, null,
                    new NormalizedValue(request.assertion().assertionId(), ATTRIBUTE, new CodeValue("energy", code),
                            new RuleVersion("test-normalization", 1)));
        }, corrections, new ResolutionRuleRegistry(List.of(rule)));
    }

    private static SourceUsagePolicy policy(String source, boolean allowed) {
        return new SourceUsagePolicy(source + "-policy", new SourceId(source), "1", Set.of(SourceContentType.ATTRIBUTE),
                allowed ? Set.of(ProjectionSurface.NUDGER_WEB) : Set.of(), NOW.minus(Duration.ofDays(1)), null,
                Duration.ofDays(1), MediaCachePolicy.NONE, AttributionRequirement.NONE, RedistributionPolicy.ALLOWED,
                LocalDate.of(2026, 9, 1), allowed ? PolicyReviewState.REVIEWED : PolicyReviewState.UNREVIEWED, null,
                List.of(URI.create("urn:o4g:policy:" + source)));
    }

    private static SourceRecordHead head(String source, String id, String value, GtinMatchConfidence confidence) {
        SourceRecordKey key = SourceRecordKey.of(source, id);
        SourceAssertion assertion = SourceAssertion.of(key, new org.open4goods.datareference.model.SourceFieldId(source, "energy", "1"),
                0, SourceContentType.ATTRIBUTE, ScalarEvidence.of(value));
        return new SourceRecordHead(key, "1", null, NOW.minusSeconds(10), NOW.minusSeconds(5), null,
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, new PayloadHash("SHA-256", "aa"),
                URI.create("urn:o4g:evidence:" + source), new SourceUsagePolicyRef(source + "-policy", "1"),
                List.of(new GtinLink(GTIN, confidence, GtinMatchMethod.DECLARED_IDENTIFIER, null)), List.of(assertion));
    }

    private static CorrectionsPort noCorrections() {
        return corrections();
    }

    private static CorrectionsPort corrections(Correction... values) {
        return new CorrectionsPort() {
            @Override
            public List<Correction> findByGtin(Gtin gtin) {
                return java.util.Arrays.stream(values).filter(value -> value.gtin().equals(gtin)).toList();
            }

            @Override
            public ScanPage<Correction> scanAll(org.open4goods.datareference.port.ScanRequest request) {
                return ScanPage.last(List.of());
            }
        };
    }
}
