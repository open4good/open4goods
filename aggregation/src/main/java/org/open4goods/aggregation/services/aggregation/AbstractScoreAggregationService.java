package org.open4goods.aggregation.services.aggregation;

import java.util.HashMap;
import java.util.Map;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.data.Score;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractScoreAggregationService extends  AbstractAggregationService{

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractScoreAggregationService.class);

	private Map<String, Cardinality>  batchDatas = new HashMap<>();
	
	
	public AbstractScoreAggregationService(String logsFolder) {
		super(logsFolder);
	}


	@Override
	public void init() {		
		super.init();
		batchDatas.clear();
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
	private void processCardinality(Score score) {


		if (null == score.getValue()) {
			LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",score);
			return;
		}

		// Retrieving cardinality
		Cardinality c = (Cardinality) batchDatas.get(score.getName());
		if (null == c) {
			c = new Cardinality();
		}

		// Incrementing
		c.increment(score);

		batchDatas.put(score.getName(),c);

	
		
	}

}
