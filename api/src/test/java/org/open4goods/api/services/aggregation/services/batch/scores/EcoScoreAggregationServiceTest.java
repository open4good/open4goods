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
}
