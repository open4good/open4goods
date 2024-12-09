package org.open4goods.api.services.aggregation.services.batch.scores;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.model.data.Brand;
import org.open4goods.commons.model.data.Score;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.BrandScoreService;
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
	
	private BrandScoreService brandScoreService;
	
	private VerticalsConfigService verticalsConfigService;
	
	public Brand2ScoreAggregationService(final Logger logger, BrandService brandService, VerticalsConfigService verticalsConfigService, BrandScoreService brandScoreService) {
		super(logger);
		this.brandService = brandService;
		this.brandScoreService = brandScoreService;
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
			
			Brand brand = brandService.resolve(data.brand());
			if (null == brand || StringUtils.isEmpty(brand.getCompanyName())) {
				brandService.incrementUnknown(data.brand());
				dedicatedLogger.warn("Cannot resolve company for {}",data.brand());
				return;
			}
			
			Double score = brandScoreService.getBrandScore(brand.getCompanyName(),"sustainalytics.com").getNormalized();
			if (null == score) {
				dedicatedLogger.error("No score found for {} - {}",data.brand(), brand.getCompanyName());
				return;
			}
			
			// Processing cardinality
			incrementCardinality(BRAND_SUSTAINABILITY_SCORENAME,score);			
			Score s = new Score(BRAND_SUSTAINABILITY_SCORENAME, score);
			// Saving in product
			data.getScores().put(s.getName(),s);
		} catch (Exception e) {
			dedicatedLogger.warn("Brand to score fail for {}",data,e);
		}
		
	}
}
