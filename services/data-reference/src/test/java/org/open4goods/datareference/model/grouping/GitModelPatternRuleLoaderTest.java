package org.open4goods.datareference.model.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.registry.GitRegistryLoader;
import org.open4goods.datareference.port.CanonicalRegistryLookup;

/**
 * Loader boundary tests for GOU-198: every rejection the checked-in schema
 * and semantic validation must enforce, plus the real checked-in default
 * rule set.
 */
class GitModelPatternRuleLoaderTest {

    @Test
    void loadsTheCheckedInDefaultRuleSetAndConfirmsTheFamilyAcrossRegionalSuffixes() throws IOException {
        ModelPatternRuleRegistry registry = loader().loadDefault();

        assertThat(registry.matchFamily("Acme", new CanonicalClassId("television"), "XR-500-EU"))
                .isEqualTo(registry.matchFamily("Acme", new CanonicalClassId("television"), "XR500FR"));
        assertThat(registry.matchFamily("Acme", new CanonicalClassId("television"), "XR-500-EU"))
                .contains(new GroupId(GroupType.FAMILY, "television-acme-xr"));
    }

    @Test
    void rejectsAnUnknownCanonicalClassId() {
        String invalid = fixtureJson().replace("\"o4g:class:television\"", "\"o4g:class:not-a-real-class\"");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("unknown canonical class id");
    }

    @Test
    void rejectsAnUnknownContractProperty() {
        String invalid = fixtureJson().replace("\"reviewer\"", "\"unexpected\"");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("unknown property");
    }

    @Test
    void rejectsAMissingRequiredProperty() {
        String invalid = fixtureJson().replace(",\n    \"reviewer\" : \"catalog-review@open4goods.org\"", "");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("missing required property");
    }

    @Test
    void rejectsACatastrophicPatternThroughTheFullLoaderPipeline() {
        String invalid = fixtureJson()
                .replace("\"(?i)^(?<family>xr)[- ]?(?<model>\\\\d{3,4})(?:[- ]?(?:eu|fr|us|uk))?$\"",
                        "\"^(?<family>a+)+$\"")
                .replace("\"XR-500-EU\", \"XR500FR\", \"XR 500 US\", \"XR-650-EU\", \"xr720uk\"", "\"aaaa\"")
                .replace("\"QX-500-EU\", \"XR-PRO-TAB\", \"XR50-EU\"", "");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("catastrophic");
    }

    @Test
    void rejectsTwoAmbiguousOverlappingRulesForTheSameScope() {
        String invalid = fixtureJson().replace("} ]", """
                }, {
                    "version" : "family-acme-broad-television@1",
                    "canonicalBrand" : "acme",
                    "canonicalClass" : "o4g:class:television",
                    "pattern" : "(?i)^(?<family>[a-z]+)-?\\\\d{3,4}(?:-[a-z]{2})?$",
                    "examples" : [ "XR-500-EU" ],
                    "counterexamples" : [ ],
                    "reviewer" : "catalog-review@open4goods.org"
                } ]
                """.strip());

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("ambiguous overlapping pattern rules");
    }

    private static GitModelPatternRuleLoader loader() throws IOException {
        CanonicalRegistryLookup registry = new GitRegistryLoader().loadDefault().registry();
        return new GitModelPatternRuleLoader(registry);
    }

    private static ModelPatternRuleRegistry load(String content) throws IOException {
        return loader().load(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static String fixtureJson() {
        try (var stream = GitModelPatternRuleLoaderTest.class.getResourceAsStream("/registry/model-pattern-rules.json")) {
            if (stream == null) {
                throw new IllegalStateException("model pattern rule fixture is missing");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read model pattern rule fixture", exception);
        }
    }
}
