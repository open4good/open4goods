package org.open4goods.datareference.model.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.RuleVersion;

/**
 * Tests the reviewed family pattern rule's construction-time contract: a
 * catastrophic regex, a missing named capture, an empty capture or an
 * incorrectly-matching counterexample must all fail loudly before the rule
 * ever enters a registry (GOU-198, AC3/AC4 of GOU-49).
 */
class ModelPatternRuleTest {

    private static final CanonicalClassId TV = new CanonicalClassId("television");

    @Test
    void acceptsAWellFormedRuleAndNormalizesTheCanonicalBrand() {
        ModelPatternRule rule = rule("Acme", "(?i)^(?<family>xr)[- ]?(?<model>\\d{3,4})(?:[- ]?(?:eu|fr|us|uk))?$",
                List.of("XR-500-EU", "XR500FR", "xr 650 us"), List.of("QX-500-EU"));

        assertThat(rule.canonicalBrand()).isEqualTo("acme");
    }

    @Test
    void aRegionalSuffixDoesNotPreventTheFamilyFromMatching() {
        ModelPatternRule rule = rule("Acme", "(?i)^(?<family>xr)[- ]?(?<model>\\d{3,4})(?:[- ]?(?:eu|fr|us|uk))?$",
                List.of("XR-500-EU", "XR-500-US", "XR500UK"), List.of());

        assertThat(rule.examples()).contains("XR-500-EU", "XR-500-US", "XR500UK");
    }

    @Test
    void aSizeEmbeddedInTheModelCodeStillMatchesTheSameFamily() {
        ModelPatternRule rule = rule("Acme", "(?i)^(?<family>xr)[- ]?(?<model>\\d{3,4})(?:[- ]?(?:eu|fr|us|uk))?$",
                List.of("XR-500-EU", "XR-650-EU", "XR-720-EU"), List.of());

        assertThat(rule.examples()).hasSize(3);
    }

    @Test
    void rejectsACatastrophicRegex() {
        assertThatThrownBy(() -> rule("Acme", "^(?<family>a+)+$", List.of("aaaa"), List.of()))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("catastrophic");
    }

    @Test
    void rejectsAPatternWithoutANamedFamilyGroup() {
        assertThatThrownBy(() -> rule("Acme", "^(?<model>xr\\d{3,4})$", List.of("xr500"), List.of()))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("named 'family' capture group");
    }

    @Test
    void rejectsAnEmptyFamilyCaptureOnAnExample() {
        assertThatThrownBy(() -> rule("Acme", "^(?<family>x?)500$", List.of("500"), List.of()))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("empty family capture");
    }

    @Test
    void rejectsAnExampleThatDoesNotMatch() {
        assertThatThrownBy(() -> rule("Acme", "^(?<family>xr)\\d{3,4}$", List.of("zx500"), List.of()))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsACounterexampleThatIncorrectlyMatches() {
        assertThatThrownBy(() -> rule("Acme", "(?i)^(?<family>xr)\\d{3,4}$", List.of("xr500"), List.of("XR650")))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("incorrectly matches counterexample");
    }

    @Test
    void rejectsNoExamples() {
        assertThatThrownBy(() -> rule("Acme", "^(?<family>xr)\\d{3,4}$", List.of(), List.of()))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("at least one example");
    }

    @Test
    void rejectsABlankReviewer() {
        assertThatThrownBy(() -> new ModelPatternRule(new RuleVersion("family-acme-xr-television", 1), "Acme", TV,
                "^(?<family>xr)\\d{3,4}$", List.of("xr500"), List.of(), "  "))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("reviewer must not be blank");
    }

    private static ModelPatternRule rule(String brand, String pattern, List<String> examples, List<String> counterexamples) {
        return new ModelPatternRule(new RuleVersion("family-acme-xr-television", 1), brand, TV, pattern, examples,
                counterexamples, "catalog-review@open4goods.org");
    }
}
