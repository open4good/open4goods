package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link AbstractScoreAggregationService} shared logic.
 */
class AbstractScoreAggregationServiceTest {

    private static final class DummyScoreService extends AbstractScoreAggregationService {

        DummyScoreService() {
            super(LoggerFactory.getLogger(DummyScoreService.class));
        }

        @Override
        public void onProduct(Product data, VerticalConfig vConf) {
            // No-op: this dummy implementation only exposes protected helpers for testing.
        }
    }

    @Test
    void relativizeKeepsAbsoluteCountAndSum() throws ValidationException {
        DummyScoreService service = new DummyScoreService();

        Cardinality absoluteCardinality = new Cardinality();
        absoluteCardinality.setMin(1.0);
        absoluteCardinality.setMax(3.0);
        absoluteCardinality.setAvg(2.0);
        absoluteCardinality.setCount(4);
        absoluteCardinality.setSum(8.0);

        service.batchDatas.put("TEST", absoluteCardinality);

        Score score = new Score("TEST", 2.0);
        Cardinality absolute = new Cardinality(absoluteCardinality);
        absolute.setValue(score.getValue());
        score.setAbsolute(absolute);

        service.relativize(score);

        Cardinality relativ = score.getRelativ();
        assertThat(relativ.getCount()).isEqualTo(4);
        assertThat(relativ.getSum()).isEqualTo(8.0);
        assertThat(relativ.getMin()).isZero();
        assertThat(relativ.getMax()).isEqualTo(StandardiserService.DEFAULT_MAX_RATING);
        assertThat(relativ.getAvg()).isEqualTo(2.5);
        assertThat(relativ.getValue()).isEqualTo(2.5);
    }
}
