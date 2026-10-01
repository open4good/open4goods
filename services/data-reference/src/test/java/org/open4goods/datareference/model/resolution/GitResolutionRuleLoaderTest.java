package org.open4goods.datareference.model.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.GtinMatchMethod;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceFieldId;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.normalization.NormalizationResult;
import org.open4goods.datareference.model.normalization.NormalizationStatus;
import org.open4goods.datareference.model.registry.GitRegistryLoader;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.NormalizedValue;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.port.CanonicalRegistryLookup;
import org.open4goods.datareference.port.CorrectionsPort;
import org.open4goods.datareference.port.ScanPage;
import org.open4goods.datareference.port.ScanRequest;
import org.open4goods.datareference.service.DeterministicResolutionService;

/**
 * Loader boundary tests: every AC2/AC3 rejection, the fixture set, and the
 * real GOU-109 rule set checked in for the attributes the registry maps to
 * eprel and/or icecat.
 */
class GitResolutionRuleLoaderTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final Gtin GTIN = new Gtin("12345670");
    private static final CanonicalAttributeId WIDTH = new CanonicalAttributeId("width");
    private static final CanonicalAttributeId ENERGY = new CanonicalAttributeId("classe-energy");
    private static final CanonicalAttributeId WARRANTY = new CanonicalAttributeId("warranty");
    private static final SourceUsagePolicyRef EPREL_REF = new SourceUsagePolicyRef("eprel-public-api", "2");
    private static final SourceUsagePolicyRef ICECAT_REF = new SourceUsagePolicyRef("icecat-open-content", "2");

    @Test
    void loadsTheCheckedInRealRuleSetAndIndexesItByAttributeAndSurface() throws IOException {
        ResolutionRuleRegistry registry = loader().loadDefault();

        // classe-energy: a 12-attribute regulated-label concept (M1 family 1) ranks
        // eprel ahead of icecat on NUDGER_WEB and reserves regulatory authority to
        // eprel; icecat is not B2B_API-authorised for ATTRIBUTE content, so the
        // B2B_API rule only ever ranks eprel.
        ResolutionRule energyWeb = registry.find(ENERGY, ProjectionSurface.NUDGER_WEB).orElseThrow();
        assertThat(energyWeb.rankedSources()).containsExactly(new SourceId("eprel"), new SourceId("icecat"));
        assertThat(energyWeb.regulatoryAuthority()).isEqualTo(new SourceId("eprel"));
        ResolutionRule energyB2b = registry.find(ENERGY, ProjectionSurface.B2B_API).orElseThrow();
        assertThat(energyB2b.rankedSources()).containsExactly(new SourceId("eprel"));
        assertThat(energyB2b.regulatoryAuthority()).isEqualTo(new SourceId("eprel"));

        // warranty: a declared-to-eprel concept (M1 family 2) with no icecat mapping
        // and no regulatory authority, since EPREL's own terms (article 7 §1) make
        // the supplier, not the registry, responsible for its accuracy.
        ResolutionRule warrantyWeb = registry.find(WARRANTY, ProjectionSurface.NUDGER_WEB).orElseThrow();
        assertThat(warrantyWeb.rankedSources()).containsExactly(new SourceId("eprel"));
        assertThat(warrantyWeb.regulatoryAuthority()).isNull();

        // width: an icecat-only concept (M1 family 3) has no eprel mapping, so it
        // gets a NUDGER_WEB rule only; icecat's own usage policy denies B2B_API, so
        // GOU-109 does not author a dead B2B_API rule for it.
        ResolutionRule widthWeb = registry.find(WIDTH, ProjectionSurface.NUDGER_WEB).orElseThrow();
        assertThat(widthWeb.rankedSources()).containsExactly(new SourceId("icecat"));
        assertThat(widthWeb.regulatoryAuthority()).isNull();
        assertThat(registry.find(WIDTH, ProjectionSurface.B2B_API)).isEmpty();

        // ODBL_EXPORT is granted by no source usage policy yet (GOU-104/105 scope),
        // so GOU-109 authors no rule that could never select a candidate.
        assertThat(registry.find(ENERGY, ProjectionSurface.ODBL_EXPORT)).isEmpty();
        assertThat(registry.find(WIDTH, ProjectionSurface.ODBL_EXPORT)).isEmpty();

        // The 59 attributes with no eprel or icecat mapping stay without a rule
        // rather than being guessed.
        assertThat(registry.find(new CanonicalAttributeId("color"), ProjectionSurface.NUDGER_WEB)).isEmpty();
    }

    @Test
    void resolvesEprelOnNudgerWebIndependentlyOfInputOrderByRegulatoryAuthorityAndFlagsIcecatAsAConflict() throws IOException {
        // classe-energy/NUDGER_WEB ranks eprel ahead of icecat and reserves
        // regulatory authority to eprel, so eprel wins regardless of arrival
        // order. Resolving to a publication surface is redistribution, not model
        // training or synthetic content generation (GOU-171): icecat's reviewed
        // usage policy permits redistributing ATTRIBUTE content on NUDGER_WEB, so
        // icecat genuinely competes here and a differing value correctly flags a
        // conflict, instead of icecat being silently dropped before it can be
        // compared at all.
        DeterministicResolutionService service = realDefaultService();
        SourceRecordHead eprelHead = eprelHead("A");
        SourceRecordHead icecatHead = icecatHead("B");

        var first = service.resolve(GTIN, List.of(eprelHead, icecatHead), ProjectionSurface.NUDGER_WEB, NOW);
        var second = service.resolve(GTIN, List.of(icecatHead, eprelHead), ProjectionSurface.NUDGER_WEB, NOW);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(1);
        assertThat(first.getFirst().value()).isEqualTo(new CodeValue("energy", "A"));
        assertThat(first.getFirst().conflicting()).isTrue();
    }

    @Test
    void selectsIcecatOnNudgerWebForClasseEnergyWhenNoEprelIsAvailable() throws IOException {
        // GOU-171: icecat's own reviewed usage policy excluded it from every
        // resolved value because DeterministicResolutionService used to check the
        // union of every ProhibitedUse, even though icecat only forbids
        // SYNTHETIC_CONTENT_GENERATION, not redistribution to a publication
        // surface. On 8d92f90b0 this produced an empty result even though no
        // eprel evidence exists to compete with icecat. Using the real, checked-in
        // GOU-109 rule set and the real, checked-in icecat-open-content policy.
        DeterministicResolutionService service = realDefaultService();
        SourceRecordHead icecatHead = icecatHead("B");

        var resolved = service.resolve(GTIN, List.of(icecatHead), ProjectionSurface.NUDGER_WEB, NOW);

        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().value()).isEqualTo(new CodeValue("energy", "B"));
        assertThat(resolved.getFirst().conflicting()).isFalse();
    }

    @Test
    void excludesIcecatFromB2bApiBecauseItsSurfaceGrantAndItsPolicyBothDenyIt() throws IOException {
        DeterministicResolutionService service = realDefaultService();
        SourceRecordHead eprelHead = eprelHead("A");
        SourceRecordHead icecatHead = icecatHead("B");

        var resolved = service.resolve(GTIN, List.of(icecatHead, eprelHead), ProjectionSurface.B2B_API, NOW);

        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().value()).isEqualTo(new CodeValue("energy", "A"));
        assertThat(resolved.getFirst().conflicting()).isFalse();
    }

    @Test
    void anO4gCorrectionOutranksTheRegulatoryAuthorityOnReplay() throws IOException {
        ResolutionRuleRegistry rules = loader().loadDefault();
        SourceUsagePolicyRegistry policies = SourceUsagePolicyRegistry.loadDefault();
        Correction correction = new Correction(new org.open4goods.datareference.model.AssertionId("o4g:classe-energy:review-1"),
                GTIN, ENERGY, new CodeValue("energy", "C"), NOW.minusSeconds(30), "reviewer@example.test",
                "EPREL temporarily published a known wrong value.", java.util.Set.of(ProjectionSurface.NUDGER_WEB), null);
        DeterministicResolutionService service = new DeterministicResolutionService(policies, request -> {
            String code = ((ScalarEvidence) request.assertion().evidence()).lexicalValue();
            return new NormalizationResult(request.assertion().assertionId(), new RegistryVersion(1),
                    org.open4goods.datareference.model.LanguageTag.UND, NormalizationStatus.SUCCESS, null,
                    new NormalizedValue(request.assertion().assertionId(), ENERGY, new CodeValue("energy", code),
                            new RuleVersion("test-normalization", 1)));
        }, corrections(correction), rules);

        var resolved = service.resolve(GTIN, List.of(eprelHead("A")), ProjectionSurface.NUDGER_WEB, NOW);

        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().value()).isEqualTo(new CodeValue("energy", "C"));
        assertThat(resolved.getFirst().reason()).isEqualTo(ResolutionReason.O4G_CORRECTION);
        assertThat(resolved.getFirst().conflicting()).isTrue();
    }

    @Test
    void rejectsAnUnknownCanonicalAttributeId() {
        String invalid = fixtureJson().replace("\"o4g:attribute:width\"", "\"o4g:attribute:not-a-real-attribute\"");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ResolutionRuleValidationException.class)
                .hasMessageContaining("unknown canonical attribute id");
    }

    @Test
    void rejectsAnUnknownSurface() {
        String invalid = fixtureJson().replace("\"surface\" : \"NUDGER_WEB\"", "\"surface\" : \"GLOBAL_DEFAULT\"");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ResolutionRuleValidationException.class)
                .hasMessageContaining("unknown projection surface");
    }

    @Test
    void rejectsADuplicateAttributeAndSurfacePair() {
        String invalid = fixtureJson().replace("\"o4g:attribute:classe-energy\"", "\"o4g:attribute:width\"")
                .replace("resolution-fixture-energy", "resolution-fixture-width-again");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ResolutionRuleValidationException.class)
                .hasMessageContaining("duplicate resolution rule");
    }

    @Test
    void rejectsARepeatedSourceInRankedSources() {
        String invalid = fixtureJson().replace("[ \"icecat\", \"merchant-feed.awin\" ]", "[ \"icecat\", \"icecat\" ]");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ResolutionRuleValidationException.class)
                .hasMessageContaining("rankedSources must contain each source exactly once");
    }

    @Test
    void rejectsAnEmptyRankedSourcesList() {
        String invalid = fixtureJson().replace("[ \"icecat\", \"merchant-feed.awin\" ]", "[ ]");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ResolutionRuleValidationException.class)
                .hasMessageContaining("rankedSources must not be empty");
    }

    @Test
    void rejectsARegulatoryAuthorityAbsentFromRankedSources() {
        String invalid = fixtureJson().replace("\"regulatoryAuthority\" : \"eprel\"", "\"regulatoryAuthority\" : \"icecat\"");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ResolutionRuleValidationException.class)
                .hasMessageContaining("regulatory authority must be among ranked sources");
    }

    @Test
    void rejectsASourceWithNoUsagePolicyEntry() {
        String invalid = fixtureJson().replace("\"icecat\", \"merchant-feed.awin\"", "\"icecat\", \"unlisted-source\"");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ResolutionRuleValidationException.class)
                .hasMessageContaining("no usage-policy entry");
    }

    private static GitResolutionRuleLoader loader() throws IOException {
        CanonicalRegistryLookup registry = new GitRegistryLoader().loadDefault().registry();
        SourceUsagePolicyRegistry policies = SourceUsagePolicyRegistry.loadDefault();
        return new GitResolutionRuleLoader(registry, policies);
    }

    private static DeterministicResolutionService realDefaultService() throws IOException {
        ResolutionRuleRegistry rules = loader().loadDefault();
        SourceUsagePolicyRegistry policies = SourceUsagePolicyRegistry.loadDefault();
        return new DeterministicResolutionService(policies, request -> {
            String code = ((ScalarEvidence) request.assertion().evidence()).lexicalValue();
            return new NormalizationResult(request.assertion().assertionId(), new RegistryVersion(1),
                    org.open4goods.datareference.model.LanguageTag.UND, NormalizationStatus.SUCCESS, null,
                    new NormalizedValue(request.assertion().assertionId(), ENERGY, new CodeValue("energy", code),
                            new RuleVersion("test-normalization", 1)));
        }, noCorrections(), rules);
    }

    private static SourceRecordHead eprelHead(String value) {
        return head(new SourceId("eprel"), "eprel-1", value, EPREL_REF);
    }

    private static SourceRecordHead icecatHead(String value) {
        return head(new SourceId("icecat"), "icecat-1", value, ICECAT_REF);
    }

    private static SourceRecordHead head(SourceId sourceId, String recordId, String value, SourceUsagePolicyRef policyRef) {
        SourceRecordKey key = SourceRecordKey.of(sourceId.value(), recordId);
        SourceAssertion assertion = SourceAssertion.of(key, new SourceFieldId(sourceId.value(), "energy", "1"), 0,
                SourceContentType.ATTRIBUTE, ScalarEvidence.of(value));
        return new SourceRecordHead(key, "1", null, NOW.minusSeconds(10), NOW.minusSeconds(5), null,
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, new PayloadHash("SHA-256", "aa"),
                URI.create("urn:o4g:evidence:" + sourceId.value()), policyRef,
                List.of(new GtinLink(GTIN, GtinMatchConfidence.EXACT, GtinMatchMethod.DECLARED_IDENTIFIER, null)),
                List.of(assertion));
    }

    private static ResolutionRuleRegistry load(String content) throws IOException {
        return loader().load(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static String fixtureJson() {
        try (var stream = GitResolutionRuleLoaderTest.class.getResourceAsStream("/registry/resolution-rules-fixture.json")) {
            if (stream == null) {
                throw new IllegalStateException("resolution rule fixture is missing");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read resolution rule fixture", exception);
        }
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
            public ScanPage<Correction> scanAll(ScanRequest request) {
                return ScanPage.last(List.of());
            }
        };
    }
}
