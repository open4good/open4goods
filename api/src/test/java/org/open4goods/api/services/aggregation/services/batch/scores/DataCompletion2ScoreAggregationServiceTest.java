package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
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
}
