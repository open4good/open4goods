package org.open4goods.datareference.model.grouping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests the static nested-quantifier ReDoS guard in isolation from {@link ModelPatternRule}. */
class NestedQuantifierGuardTest {

    @Test
    void flagsAQuantifiedGroupContainingAPlusQuantifiedAtom() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("^(a+)+$")).isTrue();
    }

    @Test
    void flagsAQuantifiedGroupContainingAStarQuantifiedCharacterClass() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("([a-z]+)*")).isTrue();
    }

    @Test
    void flagsAnOpenEndedBraceQuantifierOnAQuantifiedGroup() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("(a+){2,}")).isTrue();
    }

    @Test
    void acceptsAGroupWithAnInnerQuantifierThatIsNotItselfRepeated() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("(a+)")).isFalse();
    }

    @Test
    void acceptsARepeatedGroupWithNoInnerQuantifier() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("(abc)+")).isFalse();
    }

    @Test
    void acceptsAnExactCountOnAQuantifiedGroup() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("(a+){3}")).isFalse();
    }

    @Test
    void acceptsAnOptionalGroupWithAnInnerQuantifier() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("(a+)?")).isFalse();
    }

    @Test
    void acceptsAQuantifierInsideACharacterClassLiterally() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("([+*]+)")).isFalse();
    }

    @Test
    void acceptsTheCheckedInFamilyPatternShape() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier(
                "(?i)^(?<family>xr)[- ]?(?<model>\\d{3,4})(?:[- ]?(?:eu|fr|us|uk))?$")).isFalse();
    }

    /**
     * Documents the known gap this static check does not cover: overlapping
     * alternation inside a quantified group is exponential but contains no
     * nested quantifier, so it is not flagged here. {@link ModelPatternRuleRegistry}'s
     * runtime match-time bound is what catches this shape in practice -- see
     * {@code ModelPatternRuleRegistryTest#boundsMatchTimeAgainstAnOverlappingAlternationPattern}.
     */
    @Test
    void doesNotDetectOverlappingAlternationInAQuantifiedGroup() {
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("(a|a)+")).isFalse();
        assertThat(NestedQuantifierGuard.hasNestedQuantifier("(a|ab)+")).isFalse();
    }
}
