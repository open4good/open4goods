package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.BrandScore;
import org.open4goods.brand.service.BrandScoreService;
import org.open4goods.brand.service.BrandService;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

/**
 * Create a score based on brand sustainality evaluations
 * TODO : Needs evolution to handle multiple brand score providers. (have to go through an intermediate score) 
 * @author goulven
 *
 */
public class SustainalyticsAggregationService extends AbstractScoreAggregationService {

	private static final String BRAND_SUSTAINABILITY_SCORENAME = "BRAND_SUSTAINALYTICS_SCORING";

	public static final String RATING = "rating";

	public static final String RISK_LEVEL = "risk-level";

	public static final String COMPANY_URL = "url";

	private BrandService brandService;
	
	private BrandScoreService brandScoreService;
	
	
	public SustainalyticsAggregationService(final Logger logger, BrandService brandService, VerticalsConfigService verticalsConfigService, BrandScoreService brandScoreService) {
		super(logger);
		this.brandService = brandService;
		this.brandScoreService = brandScoreService;
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
				dedicatedLogger.warn("Cannot resolve brand or company for {}",data.brand());
				return;
			}
			
			BrandScore brandResult = brandScoreService.getBrandScore(brand.getCompanyName(),"sustainalytics.com");
			if (null == brandResult) {
				dedicatedLogger.error("No score found for {} - {}",data.brand(), brand.getCompanyName());
				return;
			}
			Double score = brandResult.getNormalized();
			
			// Processing cardinality
			incrementCardinality(BRAND_SUSTAINABILITY_SCORENAME,score);			
			Score s = new Score(BRAND_SUSTAINABILITY_SCORENAME, score);
			
			// Setting metadatas
			Map<String, String> metadatas = new HashMap<>();
			metadatas.put(RATING, brandResult.getScoreValue());
			metadatas.put(RISK_LEVEL, getRiskLevel(brandResult));
			metadatas.put(COMPANY_URL, brandResult.getUrl());
			
			s.setMetadatas(metadatas);
			// Saving in product
			data.getScores().put(s.getName(),s);
		} catch (Exception e) {
			dedicatedLogger.warn("Brand to score fail for {}",data,e);
		}
	}

	/**
	 * Compute the sustainalytics risk level from sustainalytics range.
	 * For the official scale, see: https://www.sustainalytics.com/corporate-solutions/esg-solutions/esg-risk-ratings
	 * 
	 * @param brandResult the result object containing the score value
	 * @return the risk level as a lowercase single word (e.g., negligible, low, medium, high, severe)
	 */
        String getRiskLevel(BrandScore brandResult) {
            if (brandResult == null || StringUtils.isBlank(brandResult.getScoreValue())) {
                return "unknown";
            }

            Double sustainalyticsRating;
            try {
                sustainalyticsRating = Double.valueOf(brandResult.getScoreValue());
            } catch (NumberFormatException e) {
                dedicatedLogger.warn("Unable to parse sustainalytics rating {}", brandResult.getScoreValue());
                return "unknown";
            }

            if (sustainalyticsRating >= 0 && sustainalyticsRating <= 9.9) {
                return "negligible";
            } else if (sustainalyticsRating >= 10 && sustainalyticsRating <= 19.9) {
                return "low";
            } else if (sustainalyticsRating >= 20 && sustainalyticsRating <= 29.9) {
                return "medium";
            } else if (sustainalyticsRating >= 30 && sustainalyticsRating <= 39.9) {
                return "high";
            } else if (sustainalyticsRating >= 40) {
                return "severe";
            }

            return "unknown";
        }

}
