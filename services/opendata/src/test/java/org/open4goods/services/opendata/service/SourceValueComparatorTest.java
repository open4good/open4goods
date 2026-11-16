package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SourceValueComparatorTest {

    @Test
    void warrantyInMonthsMatchesPublishedYears() {
        assertThat(SourceValueComparator.matches("WARRANTY", "24 mois", "2")).isTrue();
        assertThat(SourceValueComparator.matches("WARRANTY", "2 ans", "2")).isTrue();
    }

    @Test
    void weightInGramsAndDecimalCommaMatchPublishedKilograms() {
        assertThat(SourceValueComparator.matches("WEIGHT", "52000 g", "52")).isTrue();
        assertThat(SourceValueComparator.matches("WEIGHT", "5,20 kg", "5.2")).isTrue();
    }

    @Test
    void otherValueDoesNotMatch() {
        assertThat(SourceValueComparator.matches("WARRANTY", "12 mois", "2")).isFalse();
        assertThat(SourceValueComparator.matches("WEIGHT", "51000 g", "52")).isFalse();
        assertThat(SourceValueComparator.matches("WEIGHT", "lourd", "52")).isFalse();
    }

    @Test
    void otherCriteriaKeepTextComparison() {
        assertThat(SourceValueComparator.matches("CLASSE_ENERGY", "a", "A")).isTrue();
        assertThat(SourceValueComparator.matches("CLASSE_ENERGY", "24 mois", "2")).isFalse();
    }
}
