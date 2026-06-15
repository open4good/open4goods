package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class BrandScoresAggregationServiceTest {

    private final BrandScoresAggregationService service = new BrandScoresAggregationService(
            LoggerFactory.getLogger(BrandScoresAggregationServiceTest.class), null);

    @Test
    void scoreNameIsDerivedFromProvider() {
        assertThat(BrandScoresAggregationService.scoreName("cdp")).isEqualTo("BRAND_CDP_SCORING");
        assertThat(BrandScoresAggregationService.scoreName("good-on-you")).isEqualTo("BRAND_GOOD_ON_YOU_SCORING");
    }
}
