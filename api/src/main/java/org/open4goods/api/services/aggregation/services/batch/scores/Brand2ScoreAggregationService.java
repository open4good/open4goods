package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.data.Score;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.VerticalsConfigService;
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
	
	private VerticalsConfigService verticalsConfigService;
	
	public Brand2ScoreAggregationService(final Logger logger, BrandService brandService, VerticalsConfigService verticalsConfigService) {
		super(logger);
		this.brandService = brandService;
		this.verticalsConfigService = verticalsConfigService;
	}


	
	

	@Override
	public void onProduct(Product data, VerticalConfig vConf) {

		// Enforce score removing
		data.getScores().remove(BRAND_SUSTAINABILITY_SCORENAME);
		
		
		if (StringUtils.isEmpty(data.brand())) {
			return;
		}
		
		try {
			VerticalConfig vertical = verticalsConfigService.getConfigByIdOrDefault(data.getVertical());
			
			String company = vertical.resolveCompany(data.brand());
			
			// TODO : Handle aggragtion, for multiple brand RSE score providers
			Double score = brandService.getBrandScore(company,"sustainalytics.com");
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
}
