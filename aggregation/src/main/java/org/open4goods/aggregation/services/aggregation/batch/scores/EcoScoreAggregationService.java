package org.open4goods.aggregation.services.aggregation.batch.scores;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;

/**
 * Create an ecoscore based on existing scores aggregations (based on config)
 * @author goulven
 *
 */
public class EcoScoreAggregationService extends AbstractScoreAggregationService {

	private static final String ECOSCORE_SCORENAME = "ECOSCORE";
	private final Map<String, String> ecoScoreconfig;

	public EcoScoreAggregationService(final Map<String, String> ecoScoreconfig,  final String logsFolder,boolean toConsole) {
		super(logsFolder, toConsole);
		this.ecoScoreconfig = ecoScoreconfig;
	}



	@Override
	public void onProduct(Product data) {

		
		
		if (StringUtils.isEmpty(data.brand())) {
			return;
		}
		
		try {
			Double score = generateEcoScore(data.getScores());

			// Processing cardinality
			processCardinality(ECOSCORE_SCORENAME,score);			
			Score s = new Score(ECOSCORE_SCORENAME, score);
			// Saving in product
			data.getScores().put(s.getName(),s);
		} catch (ValidationException e) {
			dedicatedLogger.warn("Brand to score fail for {}",data,e);
		}								
		
	}



	private Double generateEcoScore(Map<String, Score> scores) throws ValidationException {
		
		
		double va = 0.0;
		for (String config :  ecoScoreconfig.keySet()) {
			Score score = scores.get(config);
			
			if (null == score) {
				throw new ValidationException ("EcoScore rating cannot proceed, missing subscore : " + config);
			} 			
			va += score.getValue() * Double.valueOf(ecoScoreconfig.get(config));
		}
		
		
	
		
		return va;
	}




}
