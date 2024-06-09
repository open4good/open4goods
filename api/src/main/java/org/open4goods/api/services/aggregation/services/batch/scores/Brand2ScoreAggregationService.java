package org.open4goods.api.services.aggregation.services.batch.scores;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;
import org.open4goods.services.BrandService;
import org.slf4j.Logger;

/**
 * Create a score based on brand sustainality evaluations
 * TODO : Needs evolution to handle multiple brand score providers. (have to go through an intermediate score) 
 * @author goulven
 *
 */
public class Brand2ScoreAggregationService extends AbstractScoreAggregationService {

	private static final String BRAND_SUSTAINABILITY_SCORENAME = "BRAND_SUSTAINABILITY";

	private BrandService brandService;
	
	public Brand2ScoreAggregationService(final Logger logger, BrandService brandService) {
		super(logger);
		this.brandService = brandService;
	}


	
	

	@Override
	public void onProduct(Product data, VerticalConfig vConf) {

		
		if (StringUtils.isEmpty(data.brand())) {
			return;
		}
		
		try {
			Double score = generateScoreFromBrand(data.brand());
			if (null == score) {
				dedicatedLogger.error("No score found for brand {}",data.brand());
				return;
			}
			
			// Processing cardinality
			incrementCardinality(BRAND_SUSTAINABILITY_SCORENAME,score);			
			Score s = new Score(BRAND_SUSTAINABILITY_SCORENAME, score);
			// Saving in product
			data.getScores().put(s.getName(),s);
		} catch (ValidationException e) {
			dedicatedLogger.warn("Brand to score fail for {}",data,e);
		}								
		
		
	}


	// TODO : complete with real datas
	private Double generateScoreFromBrand(String brand) {
		
		// TODO : involve when multiple brands score providers
		return brandService.getBrandScore(brand,"sustainalytics.com");
	}




}
