package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.rating.Cardinality;
import org.slf4j.LoggerFactory;

class AbstractScoreAggregationServiceTest {

    @Test
    void relativizeReturnsBoundedValueWhenMinEqualsMax() throws Exception {
        DataCompletion2ScoreAggregationService service =
                new DataCompletion2ScoreAggregationService(LoggerFactory.getLogger(AbstractScoreAggregationServiceTest.class));

        Cardinality cardinality = new Cardinality();
        cardinality.setMin(10d);
        cardinality.setMax(10d);
        cardinality.setValue(10d);

        Double relativized = service.relativize(10d, cardinality);

        assertThat(relativized).isEqualTo(StandardiserService.DEFAULT_MAX_RATING);
    }
}
