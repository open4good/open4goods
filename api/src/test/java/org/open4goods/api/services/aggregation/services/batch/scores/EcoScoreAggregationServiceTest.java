package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link EcoScoreAggregationService} behaviour.
 */
class EcoScoreAggregationServiceTest {

    @Test
    void ecoscoreIsComputedWhenDataQualityExistsWithoutBrand() {
        Product product = new Product(2L);
        product.getScores().put("OTHER", new Score("OTHER", 3.5));

        DataCompletion2ScoreAggregationService dataQualityService =
                new DataCompletion2ScoreAggregationService(LoggerFactory.getLogger(EcoScoreAggregationServiceTest.class));

        List<Product> dataset = List.of(product);
        dataQualityService.init(dataset);
        dataQualityService.onProduct(product, new VerticalConfig());
        dataQualityService.done(dataset, new VerticalConfig());

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("DATA_QUALITY", 1.0));
        VerticalConfig vConf = new VerticalConfig();
        vConf.setImpactScoreConfig(impactScoreConfig);

        EcoScoreAggregationService ecoScoreService =
                new EcoScoreAggregationService(LoggerFactory.getLogger(EcoScoreAggregationServiceTest.class));
        ecoScoreService.init(dataset);
        ecoScoreService.onProduct(product, vConf);
        ecoScoreService.done(dataset, vConf);

        Score ecoscore = product.ecoscore();

        assertThat(ecoscore).isNotNull();
        assertThat(ecoscore.getRelativ()).isNotNull();
        assertThat(ecoscore.getRelativ().getValue())
                .isEqualTo(product.getScores().get("DATA_QUALITY").getRelativ().getValue());
    }

    @Test
    void ecoscoreFallsBackToAbsoluteWhenRelativizationMissing() {
        Product product = new Product(3L);
        Score baseScore = new Score("CRITERIA", 4.0);
        Cardinality absolute = new Cardinality();
        absolute.setMin(0d);
        absolute.setMax(10d);
        
        // Define distribution stats to have Mean=5.0 and StdDev=2.5
        // This corresponds to a dataset like {2.5, 7.5}
        absolute.setCount(2);
        absolute.setSum(10.0);
        absolute.setSumOfSquares(62.5);
        absolute.setAvg(5.0);
        
        absolute.setValue(4d);
        baseScore.setAbsolute(absolute);
        product.getScores().put("CRITERIA", baseScore);

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("CRITERIA", 1.0));
        VerticalConfig vConf = new VerticalConfig();
        vConf.setImpactScoreConfig(impactScoreConfig);

        EcoScoreAggregationService ecoScoreService =
                new EcoScoreAggregationService(LoggerFactory.getLogger(EcoScoreAggregationServiceTest.class));

        List<Product> dataset = List.of(product);
        ecoScoreService.init(dataset);
        ecoScoreService.onProduct(product, vConf);
        ecoScoreService.done(dataset, vConf);

        Score ecoscore = product.ecoscore();

        assertThat(ecoscore).isNotNull();
        assertThat(ecoscore.getAbsolute().getValue()).isEqualTo(2.0);
    }
    @Test
    void ecoscoreComputedWithDefaultWhenSubScoreMissing() {
        Product product = new Product(4L);
        // Product has NO scores initially

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        // Configuration expects "MISSING_CRITERIA" with weight 1.0
        impactScoreConfig.setCriteriasPonderation(Map.of("MISSING_CRITERIA", 1.0));
        VerticalConfig vConf = new VerticalConfig();
        vConf.setImpactScoreConfig(impactScoreConfig);

        EcoScoreAggregationService ecoScoreService =
                new EcoScoreAggregationService(LoggerFactory.getLogger(EcoScoreAggregationServiceTest.class));

        List<Product> dataset = List.of(product);
        ecoScoreService.init(dataset);
        ecoScoreService.onProduct(product, vConf); // onProduct does nothing for EcoScore, but calling for consistency
        ecoScoreService.done(dataset, vConf);

        Score ecoscore = product.ecoscore();

        // Previously this would be null. Now we expect it to be 0.0 (missing score = 0 contribution)
        assertThat(ecoscore).isNotNull();
        assertThat(ecoscore.getAbsolute().getValue()).isEqualTo(0.0);
    }
}
