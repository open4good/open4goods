package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.scoring.ScoreMissingValuePolicy;
import org.open4goods.model.vertical.scoring.ScoreNormalizationConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationMethod;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;
import org.slf4j.LoggerFactory;

class EcoScoreResilienceTest {

    @Test
    void ecoscoreIsComputedEvenIfZeroWeightSubscoreIsMissing() {
        Product product = new Product(100L);
        product.getScores().put("REAL_SCORE", new Score("REAL_SCORE", 4.0));

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        // REAL_SCORE has weight 1.0, MISSING_SCORE has weight 0.0
        impactScoreConfig.setCriteriasPonderation(Map.of(
            "REAL_SCORE", 1.0,
            "MISSING_SCORE", 0.0
        ));
        VerticalConfig vConf = new VerticalConfig();
        vConf.setImpactScoreConfig(impactScoreConfig);
        vConf.setAttributesConfig(buildAttributesConfig());

        EcoScoreAggregationService ecoScoreService =
                new EcoScoreAggregationService(LoggerFactory.getLogger(EcoScoreResilienceTest.class));
        
        List<Product> dataset = List.of(product);
        ecoScoreService.init(dataset);
        ecoScoreService.onProduct(product, vConf);
        ecoScoreService.done(dataset, vConf);

        Score ecoscore = product.ecoscore();

        assertThat(ecoscore).isNotNull();
        // It should have been computed using REAL_SCORE only
    }

    @Test
    void ecoscoreIsComputedIfNonZeroWeightSubscoreIsMissing() {
        Product product = new Product(101L);
        product.getScores().put("REAL_SCORE", new Score("REAL_SCORE", 4.0));

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        // Both have weight > 0
        impactScoreConfig.setCriteriasPonderation(Map.of(
            "REAL_SCORE", 0.5,
            "MISSING_SCORE", 0.5
        ));
        VerticalConfig vConf = new VerticalConfig();
        vConf.setImpactScoreConfig(impactScoreConfig);
        vConf.setAttributesConfig(buildAttributesConfig());

        EcoScoreAggregationService ecoScoreService =
                new EcoScoreAggregationService(LoggerFactory.getLogger(EcoScoreResilienceTest.class));
        
        List<Product> dataset = List.of(product);
        ecoScoreService.init(dataset);
        ecoScoreService.onProduct(product, vConf);
        ecoScoreService.done(dataset, vConf);

        Score ecoscore = product.ecoscore();

        // This is the new behavior (resilient)
        assertThat(ecoscore).isNotNull();
        // 4.0 * weight(2.0) + missing excluded (0.0) = 8.0
        assertThat(ecoscore.getAbsolute().getValue()).isEqualTo(8.0);
    }

    private AttributesConfig buildAttributesConfig() {
        AttributeConfig real = new AttributeConfig();
        real.setKey("REAL_SCORE");
        ScoreScoringConfig scoringConfig = new ScoreScoringConfig();
        ScoreNormalizationConfig normalizationConfig = new ScoreNormalizationConfig();
        normalizationConfig.setMethod(ScoreNormalizationMethod.SIGMA);
        scoringConfig.setNormalization(normalizationConfig);
        real.setScoring(scoringConfig);

        AttributeConfig missing = new AttributeConfig();
        missing.setKey("MISSING_SCORE");
        ScoreScoringConfig missingScoring = new ScoreScoringConfig();
        ScoreNormalizationConfig missingNormalization = new ScoreNormalizationConfig();
        missingNormalization.setMethod(ScoreNormalizationMethod.SIGMA);
        missingScoring.setNormalization(missingNormalization);
        missingScoring.setMissingValuePolicy(ScoreMissingValuePolicy.EXCLUDE);
        missing.setScoring(missingScoring);

        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(List.of(real, missing));
        return attributesConfig;
    }
}
