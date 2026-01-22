package org.open4goods.api.services.aggregation.services.batch.scores;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Create an ecoscore based on existing scores, (see yaml config files).
 * This service aggregates various sub-scores (ImpactScore) defined in the vertical configuration
 * into a single EcoScore used for ranking and display.
 * 
 * @author goulven
 *
 */
public class EcoScoreAggregationService extends AbstractScoreAggregationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EcoScoreAggregationService.class);
	private static final String ECOSCORE_SCORENAME = "ECOSCORE";

	public EcoScoreAggregationService(final Logger logger) {
		super(LOGGER);
	}

	/**
	 * Processes a single product to generate its EcoScore.
	 * If the vertical configuration defines an ImpactScore, it validates sub-scores and computes the EcoScore.
	 * The computed score is stored as an absolute score in the product, to be relativized later in the batch process.
	 * 
	 * @param data The product to process
	 * @param vConf The vertical configuration containing score definitions
	 */
	@Override
	public void onProduct(Product data, VerticalConfig vConf) {
		// Nothing to do here. EcoScore is computed in the batch phase (done()) 
		// to ensure all sub-scores are available and relativized.
	}

	/**
	 * Generates the weighted EcoScore for a product based on configured criterias.
	 * 
	 * @param product The product containing the sub-scores
	 * @param vConf The vertical configuration
	 * @return The computed EcoScore, or null if a required sub-score is missing
	 * @throws ValidationException
	 */
	private Double generateEcoScore(Product product, VerticalConfig vConf) throws ValidationException {

		double ecoscoreVal = 0.0;
		Map<String, Score> scores = product.getScores();
		
		for (String config : vConf.getImpactScoreConfig().getCriteriasPonderation().keySet()) {
			Score score = scores.get(config);

			if (null == score) {
				Double weight = Double.valueOf(vConf.getImpactScoreConfig().getCriteriasPonderation().get(config));
				if (weight > 0.0) {
					LOGGER.warn("EcoScore rating for product {} missing subscore with positive weight : {}. Defaulting to 0.0 for this criteria.", product.getId(), config);
					// Continue with 0.0 contribution for this missing criteria. 
					// This fallback is required when NO products in the batch have this score, preventing virtualization (average calculation).
				}
				continue;
			}

			Double value = resolveRelativeValue(config, score);
			if (value == null) {
				LOGGER.warn("EcoScore subscore {} has no value used for product {}", config, product.getId());
				return null;
			}

			ecoscoreVal += value * Double.valueOf(vConf.getImpactScoreConfig().getCriteriasPonderation().get(config));
		}

		return ecoscoreVal;
	}

	/**
	 * Resolves the numeric value of a score to be used in the EcoScore calculation.
	 * Priorities:
	 * 1. Relative value
	 * 2. Absolute value (relativized if possible)
	 * 3. Raw value
	 * 
	 * @param config The score name
	 * @param score The score object
	 * @return The resolved double value, or null if unavailable
	 */
	private Double resolveRelativeValue(String config, Score score) {
		if (score.getRelativ() != null && score.getRelativ().getValue() != null) {
			return score.getRelativ().getValue();
		}

		if (score.getAbsolute() != null && score.getAbsolute().getValue() != null) {
			try {
				return relativize(score.getAbsolute().getValue(), score.getAbsolute());
			} catch (ValidationException e) {
				LOGGER.warn("EcoScore relativization failed for {} : {}", config, e.getMessage());
				return null;
			}
		}

		if (score.getValue() != null) {
			LOGGER.warn("EcoScore using raw value for {} due to missing cardinalities", config);
			return score.getValue();
		}

		LOGGER.warn("EcoScore rating cannot proceed, missing value for {}", config);
		return null;
	}

	/**
	 * Finalizes the batch processing:
	 * 1. Filters processed products.
	 * 2. Computes rankings (global position, better/worse alternatives).
	 * 3. Sets relativized EcoScore values.
	 * 
	 * @param datas Collection of products processed
	 * @param vConf Vertical configuration
	 */
	@Override
	public void done(Collection<Product> datas, VerticalConfig vConf) {

		LOGGER.info("EcoScore done() start. Products: {}", datas.size());

		if (null == vConf.getImpactScoreConfig() || vConf.getImpactScoreConfig().getCriteriasPonderation().isEmpty()) {
			LOGGER.error("No ImpactScore defined for vertical {}", vConf.getId());
			return;
		}

		// Compute the ecoscore from existing scores
		for (Product data : datas) {
			try {
				Double score = generateEcoScore(data, vConf);
				if (score == null) {
					LOGGER.warn("EcoScore rating skipped for {} due to missing sub-scores", data.getId());
					continue;
				}

				// Processing cardinality
				incrementCardinality(ECOSCORE_SCORENAME, score);

				// Saving the actual score in the product, it will be relativized after this
				// batch (see super().done())
				Score s = new Score(ECOSCORE_SCORENAME, score);
				s.setAbsolute(new Cardinality());
				s.getAbsolute().setValue(score);
				data.getScores().put(s.getName(), s);
				
			} catch (ValidationException e) {
				LOGGER.error("Ecoscore aggregation failed for {} : {}", data.getId(), e.getMessage());
			} catch (Exception e) {
				LOGGER.error("CRITICAL: Unexpected error in EcoScore generation loop for {}: {}", data.getId(), e.getMessage(), e);
				throw e;
			}
		}
		
		LOGGER.info("EcoScore computed. Calling super.done()");
		super.done(datas, vConf);
		LOGGER.info("super.done() finished. Products with real ecoscore calculation...");

		List<Product> productsWithRealEcoScore = datas.stream().filter(this::hasRealEcoScore).toList();
		LOGGER.info("Products with real EcoScore: {}", productsWithRealEcoScore.size());

		if (productsWithRealEcoScore.isEmpty()) {
			LOGGER.info("{} -> No real ecoscore computed, skipping ranking", this.getClass().getSimpleName());
			return;
		}

		try {
			// EcoScore stays on its absolute value (no relativisation)
			for (Product product : productsWithRealEcoScore) {
				Cardinality absolute = product.ecoscore().getAbsolute();
				product.ecoscore().setRelativ(new Cardinality(absolute));
				product.ecoscore().setValue(absolute.getValue());
			}
	
			///////////////////////
			// EcoScore ranking and "best alternativ" reach
			///////////////////////
			List<Product> sorted = new ArrayList<>(productsWithRealEcoScore);
			sorted.sort(Comparator.comparingDouble(p -> p.ecoscore().getRelativ().getValue()));
	
			int count = sorted.size();
			Long bestProductId = sorted.get(count - 1).getId();
	
			for (int i = 0; i < count; i++) {
				Product product = sorted.get(i);
				EcoScoreRanking ranking = ensureRanking(product);
				ranking.setGlobalCount(count);
				ranking.setGlobalPosition(count - i);
				ranking.setGlobalBest(bestProductId);
	
				if (i < count - 1) {
					ranking.setGlobalBetter(sorted.get(i + 1).getId());
				} else {
					ranking.setGlobalBetter(null);
				}
	
			}
		} catch (Exception e) {
			LOGGER.error("CRITICAL: Unexpected error in EcoScore ranking/finishing: {}", e.getMessage(), e);
			throw e;
		}
		LOGGER.info("EcoScore done() finished.");

	}

	private EcoScoreRanking ensureRanking(Product product) {
		EcoScoreRanking ranking = product.getRanking();
		if (null == ranking) {
			ranking = new EcoScoreRanking();
			product.setRanking(ranking);
		}
		return ranking;
	}

	private boolean hasRealEcoScore(Product product) {
		Score ecoscore = product.ecoscore();
		return ecoscore != null && !Boolean.TRUE.equals(ecoscore.getVirtual()) && ecoscore.getAbsolute() != null && ecoscore.getAbsolute().getValue() != null;
	}

}
