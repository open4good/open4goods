package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.rating.Cardinality;
import org.slf4j.LoggerFactory;

class AbstractScoreAggregationServiceTest {

    @Test
    void relativizeReturnsBoundedValueWhenMinEqualsMax() throws Exception {
        DataCompletion2ScoreAggregationService service =
                new DataCompletion2ScoreAggregationService(LoggerFactory.getLogger(AbstractScoreAggregationServiceTest.class));

        Cardinality cardinality = new Cardinality();
        cardinality.setMin(10d);
        cardinality.setMax(10d);
        cardinality.setValue(10d);
        // Ensure proper statistics for Sigma Scoring (Sigma = 0)
        cardinality.setCount(10);
        cardinality.setSum(100d);
        cardinality.setSumOfSquares(1000d); // 10 * 10^2 = 1000. Variance = 0.

        Double relativized = service.relativize(10d, cardinality);

        // With Sigma Scoring, if everyone is equal (Sigma=0), everyone is Average (2.5/5)
        assertThat(relativized).isEqualTo(StandardiserService.DEFAULT_MAX_RATING / 2.0);
    }
    
    @Test
    void relativizeHandlesOutliersUsingSigma() throws Exception {
        DataCompletion2ScoreAggregationService service =
                new DataCompletion2ScoreAggregationService(LoggerFactory.getLogger(AbstractScoreAggregationServiceTest.class));

        Cardinality cardinality = new Cardinality();
        // Dataset: {2, 2, 2, 2, 12}
        // Count: 5
        // Sum: 20 -> Mean = 4
        // SumSq: 4+4+4+4+144 = 160
        // Variance: (160/5) - 4^2 = 32 - 16 = 16 -> Sigma = 4
        // Bounds (k=2): Mean +/- 8 -> [-4, 12]
        
        cardinality.setCount(5);
        cardinality.setSum(20d);
        cardinality.setSumOfSquares(160d);
        // Min/Max/Avg handled by service or cardinality helpers, but we set them for consistency
        cardinality.setMin(2d);
        cardinality.setMax(12d);
        cardinality.setAvg(4d);
        
        // Calculate Score for "Standard" product (Value = 2)
        // Norm: (2 - (-4)) / (12 - (-4)) = 6 / 16 = 0.375
        // Score: 0.375 * 5 = 1.875
        Double scoreForStandard = service.relativize(2d, cardinality);
        assertThat(scoreForStandard).isEqualTo(1.875);
        
        // Calculate Score for "Outlier" product (Value = 12)
        // Norm: (12 - (-4)) / 16 = 1.0
        // Score: 5.0
        Double scoreForOutlier = service.relativize(12d, cardinality);
        assertThat(scoreForOutlier).isEqualTo(5.0);
    }
}
