package org.open4goods.brand.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class CompanyScoreTest {

    @Test
    void normalizesHigherIsBetterScale() {
        CompanyScore score = new CompanyScore();
        score.setValue(7.0);
        score.setScale(new ScoreScale(0.0, 8.0, true));

        assertThat(score.normalized()).isCloseTo(87.5, within(0.001));
    }

    @Test
    void invertsWhenLowerIsBetter() {
        // Retired lower-is-better risk scale: lower raw risk is better.
        CompanyScore score = new CompanyScore();
        score.setValue(10.0);
        score.setScale(new ScoreScale(0.0, 50.0, false));

        // 10/50 = 20% risk -> 80% goodness
        assertThat(score.normalized()).isCloseTo(80.0, within(0.001));
    }

    @Test
    void clampsOutOfRangeValues() {
        CompanyScore score = new CompanyScore();
        score.setValue(120.0);
        score.setScale(new ScoreScale(0.0, 100.0, true));

        assertThat(score.normalized()).isEqualTo(100.0);
    }

    @Test
    void returnsNullWhenScaleMissing() {
        CompanyScore score = new CompanyScore();
        score.setValue(7.0);

        assertThat(score.normalized()).isNull();
    }
}
