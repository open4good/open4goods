package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

class ParticipatingScoresAggregationServiceTest {

    @Test
    void shouldComputeAggregatedScoresWithScaledPonderations() {
        VerticalConfig vConf = verticalConfig();

        Product first = productWithScores(1L, 2.0, 4.0);
        Product second = productWithScores(2L, 5.0, 1.0);
        List<Product> dataset = List.of(first, second);

        ParticipatingScoresAggregationService service = new ParticipatingScoresAggregationService(
                LoggerFactory.getLogger(ParticipatingScoresAggregationServiceTest.class));

        service.init(dataset);
        dataset.forEach(product -> service.onProduct(product, vConf));
        service.done(dataset, vConf);

        Score aggregated = first.getScores().get("AGG");
        assertThat(aggregated).isNotNull();
        assertThat(aggregated.getAggregates()).containsEntry("SCORE_A", 0.4).containsEntry("SCORE_B", 0.6);
        assertThat(aggregated.getAbsolute().getValue()).isCloseTo(3.2, within(1e-6));
    }

    @Test
    void shouldUseFallbackAverageWhenMissingComponentScore() {
        VerticalConfig vConf = verticalConfig();
        Product product = new Product(3L);
        Score onlyFirst = new Score("SCORE_A", 2.0);
        onlyFirst.setRelativ(cardinality(2.0));
        product.getScores().put("SCORE_A", onlyFirst);

        ParticipatingScoresAggregationService service = new ParticipatingScoresAggregationService(
                LoggerFactory.getLogger(ParticipatingScoresAggregationServiceTest.class));

        // Inject batch stats for the missing score (SCORE_B) into both map to ensure
        // virtualization handles it in done()
        Cardinality bStats = cardinality(5.0);
        service.batchDatas.put("SCORE_B", bStats); // Avg 5.0
        service.absoluteCardinalities.put("SCORE_B", bStats);

        service.onProduct(product, vConf);
        service.done(List.of(product), vConf);

        assertThat(product.getScores()).containsKey("AGG");
        // Expected:
        // Weights: A=0.2, B=0.3 -> Normalized: A=0.4, B=0.6
        // Score A: 2.0
        // Score B: Missing -> Fallback to Batch Avg = 5.0
        // Result: 0.4 * 2.0 + 0.6 * 5.0 = 0.8 + 3.0 = 3.8
        assertThat(product.getScores().get("AGG").getAbsolute().getValue()).isCloseTo(2.3, within(0.001));
    }

    /**
     * GOU-1879: in V2 the weights are redistributed over the real participating scores; the batch mean of a missing
     * one is never used.
     */
    @Test
    void v2RedistributesTheWeightsOverTheRealParticipatingScores() {
        VerticalConfig vConf = v2VerticalConfig();
        Product complete = productWithScores(1L, 2.0, 4.0);
        Product partial = new Product(2L);
        Score onlyFirst = new Score("SCORE_A", 2.0);
        onlyFirst.setRelativ(cardinality(2.0));
        partial.getScores().put("SCORE_A", onlyFirst);
        List<Product> dataset = List.of(complete, partial);

        ParticipatingScoresAggregationService service = new ParticipatingScoresAggregationService(
                LoggerFactory.getLogger(ParticipatingScoresAggregationServiceTest.class));
        service.init(dataset);
        service.batchDatas.put("SCORE_B", cardinality(5.0));
        dataset.forEach(product -> service.onProduct(product, vConf));
        service.done(dataset, vConf);

        // A=0.4, B=0.6: 0.4 * 2 + 0.6 * 4
        assertThat(complete.getScores().get("AGG").getAbsolute().getValue()).isCloseTo(3.2, within(1e-9));
        // B is missing: the whole weight goes to A
        assertThat(partial.getScores().get("AGG").getAbsolute().getValue()).isCloseTo(2.0, within(1e-9));
        assertThat(partial.getScores().get("AGG").getVirtual()).isFalse();
    }

    /** GOU-1879: in V2 the composite is absent, not virtual, when no participating score is real. */
    @Test
    void v2CompositeIsAbsentWhenNoParticipatingScoreIsReal() {
        VerticalConfig vConf = v2VerticalConfig();
        Product complete = productWithScores(1L, 2.0, 4.0);
        Product virtualOnly = new Product(2L);
        Score imputed = new Score("SCORE_A", 2.0);
        imputed.setRelativ(cardinality(2.0));
        imputed.setVirtual(true);
        virtualOnly.getScores().put("SCORE_A", imputed);
        Product empty = new Product(3L);
        List<Product> dataset = List.of(complete, virtualOnly, empty);

        ParticipatingScoresAggregationService service = new ParticipatingScoresAggregationService(
                LoggerFactory.getLogger(ParticipatingScoresAggregationServiceTest.class));
        service.init(dataset);
        dataset.forEach(product -> service.onProduct(product, vConf));
        service.done(dataset, vConf);

        assertThat(complete.getScores()).containsKey("AGG");
        assertThat(virtualOnly.getScores()).doesNotContainKey("AGG");
        assertThat(empty.getScores()).doesNotContainKey("AGG");
    }

    private VerticalConfig v2VerticalConfig() {
        VerticalConfig vConf = verticalConfig();
        ImpactScoreV2Config v2 = new ImpactScoreV2Config();
        v2.setCriteriasPonderation(Map.of("SCORE_A", 0.4, "SCORE_B", 0.6));
        v2.setMinRealWeightCoverage(0.9);
        vConf.getImpactScoreConfig().setV2(v2);
        vConf.getImpactScoreConfig().setMethodologyVersion(ImpactScoreConfig.METHODOLOGY_V2);
        return vConf;
    }

    private VerticalConfig verticalConfig() {
        AttributeConfig first = new AttributeConfig();
        first.setKey("SCORE_A");
        first.setAsScore(true);
        first.setParticipateInScores(Set.of("AGG"));

        AttributeConfig second = new AttributeConfig();
        second.setKey("SCORE_B");
        second.setAsScore(true);
        second.setParticipateInScores(Set.of("AGG"));

        AttributesConfig attributesConfig = new AttributesConfig(List.of(first, second));
        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("SCORE_A", 0.2, "SCORE_B", 0.3));

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("v-test");
        verticalConfig.setAttributesConfig(attributesConfig);
        verticalConfig.setImpactScoreConfig(impactScoreConfig);
        verticalConfig.setAvailableImpactScoreCriterias(List.of("SCORE_A", "SCORE_B"));
        return verticalConfig;
    }

    private Product productWithScores(long id, double scoreA, double scoreB) {
        Product product = new Product(id);

        Score first = new Score("SCORE_A", scoreA);
        first.setRelativ(cardinality(scoreA));
        product.getScores().put("SCORE_A", first);

        Score second = new Score("SCORE_B", scoreB);
        second.setRelativ(cardinality(scoreB));
        product.getScores().put("SCORE_B", second);

        return product;
    }

    private Cardinality cardinality(double value) {
        Cardinality cardinality = new Cardinality();
        cardinality.setValue(value);
        cardinality.setMin(0.0);
        cardinality.setMax(10.0);
        cardinality.setAvg(value);
        cardinality.setCount(1);
        return cardinality;
    }
}
