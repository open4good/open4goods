package org.open4goods.datareference.model.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.registry.GitRegistryLoader;
import org.open4goods.datareference.port.CanonicalRegistryLookup;
import org.open4goods.datareference.port.CorrectionsPort;
import org.open4goods.datareference.port.ScanPage;
import org.open4goods.datareference.port.ScanRequest;
import org.open4goods.datareference.service.DeterministicResolutionService;

/**
 * Loader boundary tests: every AC2/AC3 rejection, plus the empty-set and
 * fixture-set behaviours.
 */
class GitResolutionRuleLoaderTest {

    private static final CanonicalAttributeId WIDTH = new CanonicalAttributeId("width");
    private static final CanonicalAttributeId ENERGY = new CanonicalAttributeId("classe-energy");

    @Test
    void loadsTheCheckedInEmptyRuleSetAndMakesTheResolverResolveNothing() throws IOException {
        ResolutionRuleRegistry registry = loader().loadDefault();

        assertThat(registry.find(WIDTH, ProjectionSurface.NUDGER_WEB)).isEmpty();
        assertThat(registry.find(ENERGY, ProjectionSurface.NUDGER_WEB)).isEmpty();

        DeterministicResolutionService service = new DeterministicResolutionService(
                SourceUsagePolicyRegistry.loadDefault(), request -> {
                    throw new AssertionError("normalization must not run when no rule can select a candidate");
                }, noCorrections(), registry);

        assertThat(service.resolve(new Gtin("12345670"), List.of(), ProjectionSurface.NUDGER_WEB, Instant.now()))
                .isEmpty();
    }

    @Test
    void loadsTheFixtureRuleSetWithItsRankedSourcesAndRegulatoryAuthority() throws IOException {
        ResolutionRuleRegistry registry = load(fixtureJson());

        ResolutionRule widthRule = registry.find(WIDTH, ProjectionSurface.NUDGER_WEB).orElseThrow();
        assertThat(widthRule.version()).isEqualTo(new RuleVersion("resolution-fixture-width", 1));
        assertThat(widthRule.rankedSources()).containsExactly(new SourceId("icecat"), new SourceId("merchant-feed.awin"));
        assertThat(widthRule.regulatoryAuthority()).isNull();

        ResolutionRule energyRule = registry.find(ENERGY, ProjectionSurface.NUDGER_WEB).orElseThrow();
        assertThat(energyRule.version()).isEqualTo(new RuleVersion("resolution-fixture-energy", 1));
        assertThat(energyRule.regulatoryAuthority()).isEqualTo(new SourceId("eprel"));

        assertThat(registry.find(WIDTH, ProjectionSurface.B2B_API)).isEmpty();
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
        return new CorrectionsPort() {
            @Override
            public List<org.open4goods.datareference.model.resolution.Correction> findByGtin(Gtin gtin) {
                return List.of();
            }

            @Override
            public ScanPage<org.open4goods.datareference.model.resolution.Correction> scanAll(ScanRequest request) {
                return ScanPage.last(List.of());
            }
        };
    }
}
