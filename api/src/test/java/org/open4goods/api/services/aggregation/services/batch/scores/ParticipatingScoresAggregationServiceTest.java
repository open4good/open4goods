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
    void shouldSkipAggregationWhenMissingComponentScore() {
        VerticalConfig vConf = verticalConfig();
        Product product = new Product(3L);
        Score onlyFirst = new Score("SCORE_A", 2.0);
        onlyFirst.setRelativ(cardinality(2.0));
        product.getScores().put("SCORE_A", onlyFirst);

        ParticipatingScoresAggregationService service = new ParticipatingScoresAggregationService(
                LoggerFactory.getLogger(ParticipatingScoresAggregationServiceTest.class));

        service.init(List.of(product));
        service.onProduct(product, vConf);
        service.done(List.of(product), vConf);

        assertThat(product.getScores()).doesNotContainKey("AGG");
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
        return cardinality;
    }
}
