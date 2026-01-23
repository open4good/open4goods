package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationMethod;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;
import org.slf4j.LoggerFactory;

class AbstractScoreAggregationServiceTest {

    private static class TestScoreAggregationService extends DataCompletion2ScoreAggregationService {

        TestScoreAggregationService() {
            super(LoggerFactory.getLogger(AbstractScoreAggregationServiceTest.class));
        }

        void registerValue(String scoreName, Double value, VerticalConfig config) throws ValidationException {
            incrementCardinality(scoreName, value, config);
        }

        Cardinality getAbsoluteCardinality(String scoreName) {
            return absoluteCardinalities.get(scoreName);
        }

        Double relativizeWithConfig(String scoreName, Double value, Cardinality abs, VerticalConfig config)
            throws ValidationException {
            return relativizeScoreValue(scoreName, value, abs, config);
        }
    }

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

    @Test
    void relativizeUsesPercentileWhenConfigured() throws Exception {
        TestScoreAggregationService service = new TestScoreAggregationService();
        String scoreName = "POWER_CONSUMPTION_STANDBY_NETWORKD";
        VerticalConfig config = buildConfigWithMethod(scoreName, ScoreNormalizationMethod.PERCENTILE);

        service.registerValue(scoreName, 0d, config);
        service.registerValue(scoreName, 0d, config);
        service.registerValue(scoreName, 0d, config);
        service.registerValue(scoreName, 1d, config);
        service.registerValue(scoreName, 1d, config);
        service.registerValue(scoreName, 2d, config);

        Cardinality absolute = service.getAbsoluteCardinality(scoreName);
        Double percentileScore = service.relativizeWithConfig(scoreName, 2d, absolute, config);

        assertThat(percentileScore).isCloseTo(4.5833, within(1e-4));
    }

    @Test
    void relativizeUsesSigmaWhenConfigured() throws Exception {
        TestScoreAggregationService service = new TestScoreAggregationService();
        String scoreName = "POWER_CONSUMPTION_STANDBY_NETWORKD";
        VerticalConfig config = buildConfigWithMethod(scoreName, ScoreNormalizationMethod.SIGMA);

        service.registerValue(scoreName, 0d, config);
        service.registerValue(scoreName, 0d, config);
        service.registerValue(scoreName, 0d, config);
        service.registerValue(scoreName, 1d, config);
        service.registerValue(scoreName, 1d, config);
        service.registerValue(scoreName, 2d, config);

        Cardinality absolute = service.getAbsoluteCardinality(scoreName);
        Double sigmaScore = service.relativizeWithConfig(scoreName, 2d, absolute, config);

        double mean = 4d / 6d;
        double variance = (6d / 6d) - (mean * mean);
        double sigma = Math.sqrt(variance);
        double lowerBound = mean - (2 * sigma);
        double upperBound = mean + (2 * sigma);
        double normalized = (2d - lowerBound) / (upperBound - lowerBound);
        double expected = normalized * StandardiserService.DEFAULT_MAX_RATING;

        assertThat(sigmaScore).isCloseTo(expected, within(1e-6));
    }

    @Test
    void relativizeUsesFixedBoundsWhenConfigured() throws Exception {
        TestScoreAggregationService service = new TestScoreAggregationService();
        String scoreName = "REPAIRABILITY_INDEX";

        VerticalConfig config = buildConfigWithMethod(scoreName, ScoreNormalizationMethod.MINMAX_FIXED);
        AttributeConfig attributeConfig = config.getAttributesConfig().getAttributeConfigByKey(scoreName);
        attributeConfig.getScoring().getNormalization().getParams().setFixedMin(0.0);
        attributeConfig.getScoring().getNormalization().getParams().setFixedMax(10.0);

        service.registerValue(scoreName, 5d, config);
        Cardinality absolute = service.getAbsoluteCardinality(scoreName);
        Double normalized = service.relativizeWithConfig(scoreName, 5d, absolute, config);

        assertThat(normalized).isEqualTo(2.5);
    }
    @Test
    void relativizePreservesStdDev() throws Exception {
        DataCompletion2ScoreAggregationService service =
                new DataCompletion2ScoreAggregationService(LoggerFactory.getLogger(AbstractScoreAggregationServiceTest.class));

        Cardinality cardinality = new Cardinality();
        // Values: 0, 10. Mean=5. Variance=25. StdDev=5.
        // SumSq = 0 + 100 = 100.
        // Sum = 10.
        // Count = 2.
        
        cardinality.setCount(2);
        cardinality.setSum(10d);
        cardinality.setSumOfSquares(100d);
        cardinality.setMin(0d);
        cardinality.setMax(10d);
        cardinality.setAvg(5d);
        
        // This relies on service.relativize creating a NEW cardinality that copies sumOfSquares
        Double relativeValue = service.relativize(5d, cardinality); // Value at Mean -> 2.5
        
        // We can't easily inspect the 'ret' cardinality created inside 'relativize' method 
        // because it interacts with 'p.getScores()'.
        // But AbstractScoreAggregationService.relativize(Double, Cardinality) only returns Double.
        // The one that updates the Score object is relativize(Score, VerticalConfig).
        
        // So we need to use a dummy Score object to test the copying side effect.
        
        org.open4goods.model.product.Score score = new org.open4goods.model.product.Score("TEST", 5d);
        score.setAbsolute(cardinality);
        // We need to inject the cardinality into batchDatas map of the service.
        java.lang.reflect.Field field = AbstractScoreAggregationService.class.getDeclaredField("batchDatas");
        field.setAccessible(true);
        java.util.Map<String, Cardinality> batchDatas = (java.util.Map<String, Cardinality>) field.get(service);
        batchDatas.put("TEST", cardinality);
        
        // We need a dummy VerticalConfig
        VerticalConfig vConf = new VerticalConfig();
        vConf.setAttributesConfig(new org.open4goods.model.vertical.AttributesConfig()); 
        
        service.relativize(score, vConf); // This calls the method that creates 'ret'
        
        assertThat(score.getRelativ()).isNotNull();
        assertThat(score.getRelativ().getStdDev()).isEqualTo(5.0);
    }

    private VerticalConfig buildConfigWithMethod(String scoreName, ScoreNormalizationMethod method) {
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey(scoreName);
        ScoreScoringConfig scoringConfig = new ScoreScoringConfig();
        ScoreNormalizationConfig normalizationConfig = new ScoreNormalizationConfig();
        normalizationConfig.setMethod(method);
        normalizationConfig.setParams(new ScoreNormalizationParams());
        scoringConfig.setNormalization(normalizationConfig);
        attributeConfig.setScoring(scoringConfig);

        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(java.util.List.of(attributeConfig));
        VerticalConfig config = new VerticalConfig();
        config.setAttributesConfig(attributesConfig);
        config.setImpactScoreConfig(new ImpactScoreConfig());
        return config;
    }

}
