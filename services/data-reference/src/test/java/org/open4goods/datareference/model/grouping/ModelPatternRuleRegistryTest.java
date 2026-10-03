package org.open4goods.datareference.model.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.RuleVersion;

/**
 * AC8 fixtures for GOU-198: a regional suffix and an embedded size must not
 * prevent a reviewed rule from confirming the same family, and two rules
 * whose examples overlap in the same canonical brand/class scope must be
 * rejected as ambiguous before either can confirm a {@code FAMILY} group.
 */
class ModelPatternRuleRegistryTest {

    private static final CanonicalClassId TV = new CanonicalClassId("television");
    private static final CanonicalClassId FRIDGE = new CanonicalClassId("refrigerator");

    private static final String XR_PATTERN = "(?i)^(?<family>xr)[- ]?(?<model>\\d{3,4})(?:[- ]?(?:eu|fr|us|uk))?$";

    @Test
    void aRegionalSuffixStillConfirmsTheSameFamilyGroup() {
        ModelPatternRuleRegistry registry = new ModelPatternRuleRegistry(
                List.of(xrRule("acme", TV, List.of("XR-500-EU", "XR-500-US"), List.of())));

        Optional<GroupId> eu = registry.matchFamily("Acme", TV, "XR-500-EU");
        Optional<GroupId> us = registry.matchFamily("Acme", TV, "XR-500-US");

        assertThat(eu).contains(new GroupId(GroupType.FAMILY, "10-television-4-acme-2-xr"));
        assertThat(eu).isEqualTo(us);
    }

    @Test
    void aSizeEmbeddedInTheModelCodeStillConfirmsTheSameFamily() {
        ModelPatternRuleRegistry registry = new ModelPatternRuleRegistry(
                List.of(xrRule("acme", TV, List.of("XR-500-EU", "XR-650-EU"), List.of())));

        Optional<GroupId> size500 = registry.matchFamily("Acme", TV, "XR-500-EU");
        Optional<GroupId> size650 = registry.matchFamily("Acme", TV, "XR-650-EU");

        assertThat(size500).isEqualTo(size650);
        assertThat(size500).contains(new GroupId(GroupType.FAMILY, "10-television-4-acme-2-xr"));
    }

    @Test
    void aDifferentCanonicalClassForTheSameBrandAndTextProducesNoMatch() {
        ModelPatternRuleRegistry registry = new ModelPatternRuleRegistry(
                List.of(xrRule("acme", TV, List.of("XR-500-EU"), List.of())));

        assertThat(registry.matchFamily("Acme", FRIDGE, "XR-500-EU")).isEmpty();
    }

    @Test
    void textThatMatchesNoRuleInScopeProducesNoMatch() {
        ModelPatternRuleRegistry registry = new ModelPatternRuleRegistry(
                List.of(xrRule("acme", TV, List.of("XR-500-EU"), List.of())));

        assertThat(registry.matchFamily("Acme", TV, "ZX-500-EU")).isEmpty();
    }

    @Test
    void rejectsTwoRulesInTheSameScopeWhoseExamplesOverlap() {
        ModelPatternRule xr = xrRule("acme", TV, List.of("XR-500-EU"), List.of());
        ModelPatternRule broad = new ModelPatternRule(new RuleVersion("family-acme-broad-television", 1), "acme", TV,
                "(?i)^(?<family>[a-z]+)-?\\d{3,4}(?:-[a-z]{2})?$", List.of("XR-500-EU", "ZX-900-FR"), List.of(),
                "catalog-review@open4goods.org");

        assertThatThrownBy(() -> new ModelPatternRuleRegistry(List.of(xr, broad)))
                .isInstanceOf(ModelPatternRuleValidationException.class)
                .hasMessageContaining("ambiguous overlapping pattern rules");
    }

    @Test
    void twoRulesForDifferentScopesNeverOverlap() {
        ModelPatternRule tvRule = xrRule("acme", TV, List.of("XR-500-EU"), List.of());
        ModelPatternRule fridgeRule = xrRule("acme", FRIDGE, List.of("XR-500-EU"), List.of());

        ModelPatternRuleRegistry registry = new ModelPatternRuleRegistry(List.of(tvRule, fridgeRule));

        assertThat(registry.matchFamily("acme", TV, "XR-500-EU"))
                .isEqualTo(Optional.of(new GroupId(GroupType.FAMILY, "10-television-4-acme-2-xr")));
        assertThat(registry.matchFamily("acme", FRIDGE, "XR-500-EU"))
                .isEqualTo(Optional.of(new GroupId(GroupType.FAMILY, "12-refrigerator-4-acme-2-xr")));
    }

    /**
     * GOU-242 AC2: {@link NestedQuantifierGuard} only rejects nested quantifiers
     * at authoring time, so a pattern built from overlapping alternation inside
     * a quantified group -- exponential, but no nested quantifier -- clears
     * construction and reaches {@link ModelPatternRuleRegistry#matchFamily}.
     * Matching it against a crafted worst-case input must still return
     * promptly instead of blocking on unbounded backtracking.
     */
    @Test
    void boundsMatchTimeAgainstAnOverlappingAlternationPattern() {
        ModelPatternRule rule = new ModelPatternRule(new RuleVersion("family-acme-redos-television", 1), "acme", TV,
                "^(?<family>(?:a|a)+)b$", List.of("aaab"), List.of(), "catalog-review@open4goods.org");
        ModelPatternRuleRegistry registry = new ModelPatternRuleRegistry(List.of(rule));
        String worstCaseInput = "a".repeat(40) + "c";

        long startNanos = System.nanoTime();
        Optional<GroupId> result = registry.matchFamily("acme", TV, worstCaseInput);
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        assertThat(result).isEmpty();
        assertThat(elapsedMillis).isLessThan(2_000L);
    }

    private static ModelPatternRule xrRule(String brand, CanonicalClassId canonicalClass, List<String> examples,
            List<String> counterexamples) {
        return new ModelPatternRule(new RuleVersion("family-acme-xr-" + canonicalClass.slug(), 1), brand, canonicalClass,
                XR_PATTERN, examples, counterexamples, "catalog-review@open4goods.org");
    }
}
