package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.services.aggregation.services.batch.scores.EcoScoreAggregationService;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EcoScoreAggregationServiceReproductionIT {

    private EcoScoreAggregationService service;
    private final Logger logger = LoggerFactory.getLogger(EcoScoreAggregationServiceReproductionIT.class);

    @BeforeEach
    void setUp() {
        service = new EcoScoreAggregationService(logger);
    }

    @Test
    void shouldFailToComputeEcoScoreWhenRequiredSubScoreIsMissingForAllProducts() {
        // Given
        VerticalConfig vConf = new VerticalConfig();
        vConf.setId("test-vertical");
        ImpactScoreConfig impactConf = new ImpactScoreConfig();
        // Require SCORE_A with weight 1.0
        impactConf.setCriteriasPonderation(Map.of("SCORE_A", 1.0));
        vConf.setImpactScoreConfig(impactConf);

        Product product = new Product(1L);
        // Product has NO scores

        // When
        service.onProduct(product, vConf); // Should log warning and return
        service.done(List.of(product), vConf);

        // Then
        // EcoScore should be missing or empty
        assertThat(product.ecoscore()).isNull();
    }
    
    @Test
    void shouldComputeEcoScoreWhenSubScoreExists() {
         // Given
        VerticalConfig vConf = new VerticalConfig();
        vConf.setId("test-vertical");
        ImpactScoreConfig impactConf = new ImpactScoreConfig();
        impactConf.setCriteriasPonderation(Map.of("SCORE_A", 1.0));
        vConf.setImpactScoreConfig(impactConf);

        Product product = new Product(1L);
        Score subScore = new Score("SCORE_A", 10.0);
        // Need absolute value for resolution?
        // generateEcoScore calls resolveRelativeValue.
        // resolveRelativeValue checks relativ, absolute, then value.
        // Score(name, value) sets value.
        
        product.getScores().put("SCORE_A", subScore);

        // When
        service.onProduct(product, vConf);
        service.done(List.of(product), vConf);

        // Then
        assertThat(product.ecoscore()).isNotNull();
    }
}
