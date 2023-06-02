package org.open4goods.aggregation.services.aggregation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.SourcedScore;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractScoreAggregationService extends AbstractAggregationService{

	private static final Logger LOGGER = LoggerFactory.getLogger(ScoresAggregationService.class);

	
	public AbstractScoreAggregationService(String logsFolder) {
		super(logsFolder);
	}




	/**
	 * Computes relativ values
	 * @param score
	 */
	private void relativize(Score score, Cardinality cardinality) {

		// Substracting unused min

		if (null == score.getValue()) {
			LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",score);
			return;
		}

		try {
			// Removing the min range
			Double minBorn = cardinality.getMin() - score.getMin();

			// Standardizing Score based on real max
			final Double max = cardinality.getMax();

			final Double value = score.getValue();
			score.setRelValue((value -minBorn) * StandardiserService.DEFAULT_MAX_RATING / (max -minBorn));

		} catch (Exception e) {
			LOGGER.warn("Relativisation failed",e);
		}

	}


	/**
	 * Computes and maintains cardinality
	 * @param Scores
	 * @param batchDatas
	 */
	private void processCardinality(Score score, Cardinality c ) {


		if (null == score.getValue()) {
			LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",r);
			return;
		}

		// Retrieving cardinality
		Cardinality c = (Cardinality) batchDatas.get(r.getName());
		if (null == c) {
			c = new Cardinality();
		}

		// Incrementing
		c.increment(r);

		batchDatas.put(getCardId(r),c);

	
		
	}

}
