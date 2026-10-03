package org.open4goods.datareference.model.grouping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests the static nested-quantifier ReDoS guard in isolation from {@link ModelPatternRule}. */
class CatastrophicRegexDetectorTest {

    @Test
    void flagsAQuantifiedGroupContainingAPlusQuantifiedAtom() {
        assertThat(CatastrophicRegexDetector.isCatastrophic("^(a+)+$")).isTrue();
    }

    @Test
    void flagsAQuantifiedGroupContainingAStarQuantifiedCharacterClass() {
        assertThat(CatastrophicRegexDetector.isCatastrophic("([a-z]+)*")).isTrue();
    }

    @Test
    void flagsAnOpenEndedBraceQuantifierOnAQuantifiedGroup() {
        assertThat(CatastrophicRegexDetector.isCatastrophic("(a+){2,}")).isTrue();
    }

    @Test
    void acceptsAGroupWithAnInnerQuantifierThatIsNotItselfRepeated() {
        assertThat(CatastrophicRegexDetector.isCatastrophic("(a+)")).isFalse();
    }

    @Test
    void acceptsARepeatedGroupWithNoInnerQuantifier() {
        assertThat(CatastrophicRegexDetector.isCatastrophic("(abc)+")).isFalse();
    }

    @Test
    void acceptsAnExactCountOnAQuantifiedGroup() {
        assertThat(CatastrophicRegexDetector.isCatastrophic("(a+){3}")).isFalse();
    }

    @Test
    void acceptsAnOptionalGroupWithAnInnerQuantifier() {
        assertThat(CatastrophicRegexDetector.isCatastrophic("(a+)?")).isFalse();
    }

    @Test
    void acceptsAQuantifierInsideACharacterClassLiterally() {
        assertThat(CatastrophicRegexDetector.isCatastrophic("([+*]+)")).isFalse();
    }

    @Test
    void acceptsTheCheckedInFamilyPatternShape() {
        assertThat(CatastrophicRegexDetector.isCatastrophic(
                "(?i)^(?<family>xr)[- ]?(?<model>\\d{3,4})(?:[- ]?(?:eu|fr|us|uk))?$")).isFalse();
    }
}
