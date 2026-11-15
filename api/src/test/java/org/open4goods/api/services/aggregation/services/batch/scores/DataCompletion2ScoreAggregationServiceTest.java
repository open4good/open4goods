package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link DataCompletion2ScoreAggregationService}.
 */
class DataCompletion2ScoreAggregationServiceTest {

    @Test
    void shouldComputeDataQualityWhenBrandMissing() {
        Product product = new Product(1L);
        // Preload a real score so data-quality counts one non-virtual score.
        product.getScores().put("OTHER", new Score("OTHER", 3.5));

        DataCompletion2ScoreAggregationService service =
                new DataCompletion2ScoreAggregationService(LoggerFactory.getLogger(DataCompletion2ScoreAggregationServiceTest.class));

        List<Product> dataset = List.of(product);
        service.init(dataset);
        service.onProduct(product, new VerticalConfig());
        service.done(dataset, new VerticalConfig());

        Score dataQuality = product.getScores().get("DATA_QUALITY");

        assertThat(dataQuality).isNotNull();
        assertThat(dataQuality.getVirtual()).isFalse();
        assertThat(dataQuality.getAbsolute()).isNotNull();
        assertThat(dataQuality.getAbsolute().getValue()).isEqualTo(1.0);
        assertThat(dataQuality.getRelativ().getValue()).isEqualTo(StandardiserService.DEFAULT_MAX_RATING / 2);
    }

    private static VerticalConfig v2Vertical(String... matrixKeys) {
        VerticalConfig vc = new VerticalConfig();
        List<AttributeConfig> attrs = new ArrayList<>();
        ImpactScoreV2Config v2 = new ImpactScoreV2Config();
        Map<String, Double> matrix = new HashMap<>();
        for (String k : matrixKeys) {
            AttributeConfig a = new AttributeConfig();
            a.setKey(k);
            a.setAsScore(true);
            attrs.add(a);
            matrix.put(k, 1.0);
        }
        for (String k : List.of("POWER_CONSUMPTION_TYPICAL", "REPAIRABILITY_INDEX")) {
            AttributeConfig a = new AttributeConfig();
            a.setKey(k);
            a.setAsScore(true);
            attrs.add(a);
        }
        v2.setCriteriasPonderation(matrix);
        vc.getImpactScoreConfig().setV2(v2);
        vc.getImpactScoreConfig().setMethodologyVersion(ImpactScoreConfig.METHODOLOGY_V2);
        vc.setAttributesConfig(new AttributesConfig(attrs));
        // Legacy list, deliberately different from the matrix: it must not be read.
        vc.setAvailableImpactScoreCriterias(List.of("POWER_CONSUMPTION_TYPICAL", "REPAIRABILITY_INDEX"));
        return vc;
    }

    private static double v2Quality(VerticalConfig vc, String... scores) {
        Product product = new Product(1L);
        for (String s : scores) {
            product.getScores().put(s, new Score(s, 3.0));
        }
        DataCompletion2ScoreAggregationService service =
                new DataCompletion2ScoreAggregationService(LoggerFactory.getLogger(DataCompletion2ScoreAggregationServiceTest.class));
        service.init(List.of(product));
        service.onProduct(product, vc);
        return product.getScores().get("DATA_QUALITY").getAbsolute().getValue();
    }

    @Test
    void v2CountsMatrixCriteriaNotAvailableList() {
        VerticalConfig washing = v2Vertical("ENERGY_CONSUMPTION_100_CYCLES");
        assertThat(v2Quality(washing, "ENERGY_CONSUMPTION_100_CYCLES", "POWER_CONSUMPTION_TYPICAL")).isEqualTo(1.0);

        VerticalConfig phone = v2Vertical("EU_REPAIRABILITY_CLASS");
        assertThat(v2Quality(phone, "EU_REPAIRABILITY_CLASS", "REPAIRABILITY_INDEX", "BRAND_X_SCORING")).isEqualTo(1.0);
    }

    @Test
    void v1CountsEveryRealScore() {
        VerticalConfig vc = v2Vertical("ENERGY_CONSUMPTION_100_CYCLES");
        vc.getImpactScoreConfig().setMethodologyVersion(ImpactScoreConfig.METHODOLOGY_V1);
        assertThat(v2Quality(vc, "ENERGY_CONSUMPTION_100_CYCLES", "POWER_CONSUMPTION_TYPICAL", "BRAND_X_SCORING")).isEqualTo(3.0);
    }
}
