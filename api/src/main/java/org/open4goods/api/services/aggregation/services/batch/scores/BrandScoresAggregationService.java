package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.BrandScore;
import org.open4goods.brand.model.Company;
import org.open4goods.brand.model.CompanyScore;
import org.open4goods.brand.service.BrandScoreService;
import org.open4goods.brand.service.BrandService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Adds brand-level ESG / ethics scores to each product, one product
 * {@link Score} per provider.
 *
 * <p>Two planes feed this service:
 * <ul>
 *   <li>the <b>volatile</b> plane — the Sustainalytics rating scraped live by the
 *       crawler into the {@code brand-scores} Elasticsearch index. This path is
 *       preserved verbatim from the former {@code SustainalyticsAggregationService}
 *       and produces {@code BRAND_SUSTAINALYTICS_SCORING} with identical metadata.</li>
 *   <li>the <b>curated</b> plane — per-provider {@link CompanyScore} entries held in
 *       the git referential ({@link Company#getScores()}), maintained by the
 *       brands-maintenance agent. Each curated provider emits
 *       {@code BRAND_<PROVIDER>_SCORING} (e.g. {@code BRAND_CDP_SCORING}) using the
 *       provider score normalised to 0-100.</li>
 * </ul>
 *
 * <p>Curated providers are purely additive: a product only gains a provider score
 * once the company actually carries that sourced score, so existing ecoscores are
 * unaffected until the referential is enriched.
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
	private final BrandScoreService brandScoreService;

	public BrandScoresAggregationService(final Logger logger, final BrandService brandService,
			final BrandScoreService brandScoreService) {
		super(logger);
		this.brandService = brandService;
		this.brandScoreService = brandScoreService;
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

			addSustainalyticsScore(data, brand, vConf);
			addCuratedScores(data, brand, vConf);
		} catch (Exception e) {
			dedicatedLogger.warn("Brand to score fail for {}", data, e);
		}
	}

	/**
	 * Live Sustainalytics path — preserved verbatim from the former
	 * {@code SustainalyticsAggregationService}.
	 */
	private void addSustainalyticsScore(Product data, Brand brand, VerticalConfig vConf) throws ValidationException {
		BrandScore brandResult = brandScoreService.getBrandScore(brand.getCompanyName(), "sustainalytics.com");
		if (brandResult == null) {
			dedicatedLogger.warn("No score found for {} - {}", data.brand(), brand.getCompanyName());
			return;
		}
		Double score = brandResult.getNormalized();

		incrementCardinality(SUSTAINALYTICS_SCORENAME, score, vConf);
		Score s = new Score(SUSTAINALYTICS_SCORENAME, score);
		s.setAbsolute(new Cardinality());
		s.getAbsolute().setValue(score);

		Map<String, String> metadatas = new HashMap<>();
		metadatas.put(RATING, brandResult.getScoreValue());
		metadatas.put(RISK_LEVEL, getRiskLevel(brandResult));
		metadatas.put(COMPANY_URL, brandResult.getUrl());

		s.setMetadatas(metadatas);
		data.getScores().put(s.getName(), s);
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
				// Sustainalytics is owned by the live plane above.
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

	/**
	 * Compute the sustainalytics risk level from the sustainalytics range.
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

		if (sustainalyticsRating < 10) {
			return "negligible";
		} else if (sustainalyticsRating < 20) {
			return "low";
		} else if (sustainalyticsRating < 30) {
			return "medium";
		} else if (sustainalyticsRating < 40) {
			return "high";
		} else {
			return "severe";
		}
	}
}
