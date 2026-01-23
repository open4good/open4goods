package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Map;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Computes the {@code DATA_QUALITY} score from the amount of real (non virtual)
 * scores already present on the product. The service operates regardless of
 * whether optional metadata such as the brand are present so downstream score
 * composition (e.g. EcoScore) always receives a data-quality subscore.
 */
public class DataCompletion2ScoreAggregationService extends AbstractScoreAggregationService {

        private static final String DATA_QUALITY_SCORENAME = "DATA_QUALITY";


        public DataCompletion2ScoreAggregationService(final Logger logger) {
                super(logger);
        }



        @Override
        /**
         * Ensures each product receives a data-quality score that counts existing
         * non-virtual scores. This method intentionally runs even when identifying
         * attributes (e.g. brand) are missing so aggregation pipelines keep the
         * data-quality metric aligned with available signals.
         */
        public void onProduct(Product data, VerticalConfig vConf) {

                try {
                        Double score = generateScoreFromDataquality(data.getScores());

                        // Processing cardinality
                        incrementCardinality(DATA_QUALITY_SCORENAME, score, vConf);
                        Score s = new Score(DATA_QUALITY_SCORENAME, score);
                        // Saving in product
                        data.getScores().put(s.getName(),s);
                } catch (ValidationException e) {
			dedicatedLogger.warn("DataQuality to score fail for {}",data,e);
		}
		
		
	}


        /**
         * The data score is the number of scores that are not virtuals
         * @param map
         * @return
         */
        private Double generateScoreFromDataquality(Map<String, Score> map) {

                return  Double.valueOf(map.values().stream().filter(e -> !e.getVirtual()).filter(e -> !e.getName().equals(DATA_QUALITY_SCORENAME)) .count());

        }


}
