package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.Company;
import org.open4goods.brand.model.CompanyScore;
import org.open4goods.brand.service.BrandService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Adds curated brand-level ESG / ethics scores to each product.
 * Each company score emits {@code BRAND_<PROVIDER>_SCORING}; the retired
 * Sustainalytics score is explicitly removed from products when encountered.
 */
public class BrandScoresAggregationService extends AbstractScoreAggregationService {

	private static final String SUSTAINALYTICS_PROVIDER = "sustainalytics";
	private static final String SUSTAINALYTICS_SCORENAME = "BRAND_SUSTAINALYTICS_SCORING";

	public static final String RATING = "rating";
	public static final String RISK_LEVEL = "risk-level";
	public static final String COMPANY_URL = "url";
	public static final String RETRIEVED_AT = "retrievedAt";
	public static final String PROVIDER = "provider";

	private final BrandService brandService;

	public BrandScoresAggregationService(final Logger logger, final BrandService brandService) {
		super(logger);
		this.brandService = brandService;
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) {

		// Enforce removal of the Sustainalytics score so a stale value never lingers.
		data.getScores().remove(SUSTAINALYTICS_SCORENAME);

		if (StringUtils.isEmpty(data.brand())) {
			return;
		}

		try {
			Brand brand = brandService.resolve(data.brand());
			if (brand == null || StringUtils.isEmpty(brand.getCompanyName())) {
				brandService.incrementUnknown(data.brand());
				dedicatedLogger.warn("Cannot resolve brand or company for {}", data.brand());
				return;
			}

			addCuratedScores(data, brand, vConf);
		} catch (Exception e) {
			dedicatedLogger.warn("Brand to score fail for {}", data, e);
		}
	}

	/**
	 * Curated providers from the git referential. Additive: emits one score per
	 * provider that carries a normalisable value.
	 */
	private void addCuratedScores(Product data, Brand brand, VerticalConfig vConf) throws ValidationException {
		Company company = brand.getCompany();
		if (company == null || company.getScores() == null) {
			return;
		}

		for (Map.Entry<String, CompanyScore> entry : company.getScores().entrySet()) {
			String provider = entry.getKey();
			if (StringUtils.isBlank(provider) || SUSTAINALYTICS_PROVIDER.equalsIgnoreCase(provider)) {
				continue;
			}
			CompanyScore curated = entry.getValue();
			Double normalized = curated == null ? null : curated.normalized();
			if (normalized == null) {
				continue;
			}

			String scoreName = scoreName(provider);
			data.getScores().remove(scoreName);
			incrementCardinality(scoreName, normalized, vConf);
			Score s = new Score(scoreName, normalized);
			s.setAbsolute(new Cardinality());
			s.getAbsolute().setValue(normalized);

			Map<String, String> metadatas = new HashMap<>();
			metadatas.put(PROVIDER, provider);
			if (curated.getRating() != null) {
				metadatas.put(RATING, curated.getRating());
			}
			if (curated.getUrl() != null) {
				metadatas.put(COMPANY_URL, curated.getUrl());
			}
			if (curated.getRetrievedAt() != null) {
				metadatas.put(RETRIEVED_AT, curated.getRetrievedAt());
			}
			s.setMetadatas(metadatas);
			data.getScores().put(scoreName, s);
		}
	}

	static String scoreName(String provider) {
		return "BRAND_" + provider.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_") + "_SCORING";
	}
}
