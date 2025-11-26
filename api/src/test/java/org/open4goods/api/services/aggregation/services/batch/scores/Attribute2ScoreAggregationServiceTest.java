package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeComparisonRule;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link Attribute2ScoreAggregationService}.
 */
class Attribute2ScoreAggregationServiceTest {

    private Attribute2ScoreAggregationService service;

    @BeforeEach
    void setUp() {
        service = new Attribute2ScoreAggregationService(LoggerFactory.getLogger(Attribute2ScoreAggregationServiceTest.class));
    }

    @Test
    void lowerScoresUseRecomputedCardinality() {
        VerticalConfig verticalConfig = buildVerticalConfig(true);

        Product better = productWithAttribute(1L, "REPAIR", "4.0");
        Product worse = productWithAttribute(2L, "REPAIR", "9.0");
        List<Product> products = List.of(better, worse);

        aggregate(products, verticalConfig);

        Score betterScore = better.getScores().get("REPAIR");
        Score worseScore = worse.getScores().get("REPAIR");

        assertThat(betterScore.getAbsolute().getMax()).isEqualTo(5.0);
        assertThat(betterScore.getAbsolute().getMin()).isEqualTo(0.0);
        assertThat(betterScore.getAbsolute().getValue()).isEqualTo(5.0);
        assertThat(worseScore.getAbsolute().getValue()).isEqualTo(0.0);
        assertThat(betterScore.getRelativ().getValue()).isEqualTo(5.0);
        assertThat(worseScore.getRelativ().getValue()).isEqualTo(0.0);
        assertThat(betterScore.on20()).isEqualTo(20L);
        assertThat(worseScore.on20()).isEqualTo(0L);
    }

    @Test
    void virtualScoresExposeAbsoluteValue() {
        VerticalConfig verticalConfig = buildVerticalConfig(false);

        Product real = productWithAttribute(1L, "REPAIR", "8.0");
        Product missing = new Product(2L);
        List<Product> products = List.of(real, missing);

        aggregate(products, verticalConfig);

        Score missingScore = missing.getScores().get("REPAIR");
        assertThat(missingScore.getValue()).isEqualTo(8.0);
        assertThat(missingScore.getAbsolute().getValue()).isEqualTo(8.0);
        assertThat(missingScore.getAbsolute().getMax()).isEqualTo(8.0);
        assertThat(missingScore.getAbsolute().getMin()).isEqualTo(8.0);
    }

    @Test
    void lowerScoresUseCanonicalKeyForSynonyms() {
        String canonicalKey = "POWER_CONSUMPTION_TYPICAL";
        String synonymKey = "CONSO_PLEINE_PUISSANCE";
        VerticalConfig verticalConfig = buildVerticalConfig(canonicalKey, synonymKey, true);

        Product efficient = productWithAttribute(1L, synonymKey, "50.0");
        Product inefficient = productWithAttribute(2L, synonymKey, "100.0");
        List<Product> products = List.of(efficient, inefficient);

        aggregate(products, verticalConfig);

        Score efficientScore = efficient.getScores().get(canonicalKey);
        Score inefficientScore = inefficient.getScores().get(canonicalKey);

        assertThat(efficient.getScores()).doesNotContainKey(synonymKey);
        assertThat(efficientScore.getValue()).isEqualTo(50.0);
        assertThat(inefficientScore.getValue()).isEqualTo(0.0);
        assertThat(efficientScore.getValue()).isGreaterThan(inefficientScore.getValue());
    }

    private void aggregate(List<Product> products, VerticalConfig verticalConfig) {
        service.init(products);
        products.forEach(product -> service.onProduct(product, verticalConfig));
        service.done(products, verticalConfig);
    }

    private static Product productWithAttribute(long id, String key, String value) {
        Product product = new Product(id);
        product.getAttributes().getIndexed().put(key, new IndexedAttribute(key, value));
        return product;
    }

    private static VerticalConfig buildVerticalConfig(boolean lowerIsBetter) {
        return buildVerticalConfig("REPAIR", null, lowerIsBetter);
    }

    private static VerticalConfig buildVerticalConfig(String key, String synonym, boolean lowerIsBetter) {
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey(key);
        attributeConfig.setAsScore(true);
        attributeConfig.setBetterIs(lowerIsBetter ? AttributeComparisonRule.LOWER : AttributeComparisonRule.GREATER);
        attributeConfig.setFilteringType(AttributeType.NUMERIC);
        if (synonym != null) {
            Map<String, Set<String>> synonyms = new HashMap<>();
            synonyms.put("all", Set.of(synonym));
            attributeConfig.setSynonyms(synonyms);
        }

        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(List.of(attributeConfig));

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setAttributesConfig(attributesConfig);
        return verticalConfig;
    }
}
