package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.brand.model.BrandScore;
import org.slf4j.LoggerFactory;

class SustainalyticsAggregationServiceTest {

    private final SustainalyticsAggregationService service = new SustainalyticsAggregationService(
            LoggerFactory.getLogger(SustainalyticsAggregationServiceTest.class), null, null, null);

    @Test
    void riskLevelIsUnknownWhenScoreMissing() {
        BrandScore brandScore = new BrandScore();
        brandScore.setScoreValue(null);

        assertThat(service.getRiskLevel(brandScore)).isEqualTo("unknown");
    }

    @Test
    void riskLevelIsUnknownWhenScoreNotNumeric() {
        BrandScore brandScore = new BrandScore();
        brandScore.setScoreValue("abc");

        assertThat(service.getRiskLevel(brandScore)).isEqualTo("unknown");
    }

    @Test
    void riskLevelIsComputedWhenScoreValid() {
        BrandScore brandScore = new BrandScore();
        brandScore.setScoreValue("15");

        assertThat(service.getRiskLevel(brandScore)).isEqualTo("low");
    }
}
