package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
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

        EcoScoreAggregationService ecoScoreService =
                new EcoScoreAggregationService(LoggerFactory.getLogger(EcoScoreResilienceTest.class));
        
        List<Product> dataset = List.of(product);
        ecoScoreService.init(dataset);
        ecoScoreService.onProduct(product, vConf);
        ecoScoreService.done(dataset, vConf);

        Score ecoscore = product.ecoscore();

        // This is the new behavior (resilient)
        assertThat(ecoscore).isNotNull();
        // 4.0 * 0.5 + 0.0 (missing) * 0.5 = 2.0
        assertThat(ecoscore.getAbsolute().getValue()).isEqualTo(2.0);
    }
}
