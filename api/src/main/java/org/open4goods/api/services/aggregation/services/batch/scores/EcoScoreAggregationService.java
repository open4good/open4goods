package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;

/**
 * Create an ecoscore based on existing scores aggregations (based on config)
 * @author goulven
 *
 */
public class EcoScoreAggregationService extends AbstractScoreAggregationService {

	private static final String ECOSCORE_SCORENAME = "ECOSCORE";

	public EcoScoreAggregationService(final Logger logger) {
		super(logger);
	}



	@Override
	public void onProduct(Product data, VerticalConfig vConf) {

		
		
		if (StringUtils.isEmpty(data.brand())) {
			return;
		}
		
		try {
			Double score = generateEcoScore(data.getScores(),vConf);

			// Processing cardinality
			processCardinality(ECOSCORE_SCORENAME,score);			
			Score s = new Score(ECOSCORE_SCORENAME, score);
			// Saving in product
			data.getScores().put(s.getName(),s);
		} catch (ValidationException e) {
			dedicatedLogger.warn("Brand to score fail for {}",data,e);
		}								
		
	}



	private Double generateEcoScore(Map<String, Score> scores, VerticalConfig vConf) throws ValidationException {
		
		
		double va = 0.0;
		for (String config :  vConf.getEcoscoreConfig().keySet()) {
			Score score = scores.get(config);
			
			if (null == score) {
				throw new ValidationException ("EcoScore rating cannot proceed, missing subscore : " + config);
			} 			
			va += score.getValue() * Double.valueOf(vConf.getEcoscoreConfig().get(config));
		}
		
		
	
		
		return va;
	}




}
