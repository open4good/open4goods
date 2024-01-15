package org.open4goods.api.services.aggregation.services.batch.scores;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.AttributesConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;

/**
 * Create a score based on brand sustainality evaluations 
 * @author goulven
 *
 */
public class Brand2ScoreAggregationService extends AbstractScoreAggregationService {

	private static final String BRAND_SUSTAINABILITY_SCORENAME = "BRAND_SUSTAINABILITY";

	public Brand2ScoreAggregationService(final Logger logger) {
		super(logger);
	}



	@Override
	public void onProduct(Product data, VerticalConfig vConf) {

		
		if (StringUtils.isEmpty(data.brand())) {
			return;
		}
		
		try {
			Double score = generateScoreFromBrand(data.brand());

			// Processing cardinality
			processCardinality(BRAND_SUSTAINABILITY_SCORENAME,score);			
			Score s = new Score(BRAND_SUSTAINABILITY_SCORENAME, score);
			// Saving in product
			data.getScores().put(s.getName(),s);
		} catch (ValidationException e) {
			dedicatedLogger.warn("Brand to score fail for {}",data,e);
		}								
		
		
	}


	// TODO : complete with real datas
	private Double generateScoreFromBrand(String brand) {
		
		double s;
		
		switch (brand) {
		case "SAMSUNG" -> s = 5.0;
		case "LG" -> s = 4.0;
		default -> s = Math.random() * 10;
		}
		
		return s;
	}




}
