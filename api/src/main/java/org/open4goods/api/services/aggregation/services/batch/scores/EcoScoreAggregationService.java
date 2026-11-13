package org.open4goods.api.services.aggregation.services.batch.scores;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.scoring.ScoreMissingValuePolicy;
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
	private static final double ECOSCORE_TARGET_SCALE = 20.0;

	/** Tolerance of the V2 coverage threshold: a coverage C is rated when {@code C >= threshold - 1e-9}. */
	static final double COVERAGE_TOLERANCE = ImpactScoreCoverage.TOLERANCE;

	/**
	 * Coverage of the active matrix by the real criteria of a product (design GOU-1878, 2.2), over the criteria
	 * weighted above 0.
	 *
	 * @param realWeight          sum of the weights of the real criteria
	 * @param totalWeight         sum of the weights of every criterion
	 * @param realCount           number of real criteria
	 * @param totalCount          number of criteria
	 * @param weightedRelativeSum sum of {@code weight x relative value} over the real criteria (0-5 scale)
	 * @param realCriteria        keys of the real criteria, in the order of the matrix
	 */
	record Coverage(double realWeight, double totalWeight, int realCount, int totalCount, double weightedRelativeSum,
			List<String> realCriteria) {

		/**
		 * The coverage C.
		 *
		 * @return {@code realWeight / totalWeight}, {@code null} when no criterion is weighted
		 */
		Double ratio() {
			return totalWeight > 0.0 ? realWeight / totalWeight : null;
		}
	}

	public EcoScoreAggregationService(final Logger logger) {
		super(logger);
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
	 * Generates the V2 ImpactScore of a product: the weights are redistributed over the real criteria,
	 * {@code 4 x sum(w x rel) / sum(w)} over them (0-20), and there is no score below the coverage threshold
	 * (design GOU-1878, 2.2).
	 *
	 * @param product the product holding the sub-scores
	 * @param vConf   the vertical configuration, in V2
	 * @return the ImpactScore, or {@code null} when the product is not rated
	 */
	private Double generateV2EcoScore(Product product, VerticalConfig vConf) {
		ImpactScoreConfig impactScoreConfig = vConf.getImpactScoreConfig();
		Coverage coverage = coverage(product, impactScoreConfig);
		if (v2NotRatedReason(coverage, impactScoreConfig.minRealWeightCoverage()) != null) {
			return null;
		}
		return (ECOSCORE_TARGET_SCALE / StandardiserService.DEFAULT_MAX_RATING) * coverage.weightedRelativeSum()
				/ coverage.realWeight();
	}

	/**
	 * Generates the weighted EcoScore for a product based on configured criterias.
	 * 
	 * @param product The product containing the sub-scores
	 * @param vConf The vertical configuration
	 * @return The computed EcoScore, or null if a required sub-score is missing
	 * @throws ValidationException
	 */
	private Double generateEcoScore(Product product, VerticalConfig vConf, Map<String, Double> normalizedWeights) throws ValidationException {

		double ecoscoreVal = 0.0;
		Map<String, Score> scores = product.getScores();
		
		for (String config : vConf.getImpactScoreConfig().activeCriteriasPonderation().keySet()) {
			Score score = scores.get(config);

			if (score == null) {
				ScoreMissingValuePolicy policy = resolveMissingValuePolicy(config, vConf);
				Double fallback = resolveMissingScoreFallback(config, vConf, policy);
				if (fallback == null) {
					continue;
				}
				Double weight = normalizedWeights.get(config);
				if (weight == null) {
					continue;
				}
				ecoscoreVal += fallback * weight;
				continue;
			}

			Double value = resolveRelativeValue(config, score);
			if (value == null) {
				LOGGER.warn("EcoScore subscore {} has no value used for product {}", config, product.getId());
				return null;
			}

			Double weight = normalizedWeights.get(config);
			if (weight == null) {
				continue;
			}

			ecoscoreVal += value * weight;
		}

		return ecoscoreVal;
	}

	/**
	 * Finalizes the batch processing:
	 * 1. Filters processed products.
	 * 2. Computes rankings (global position, better/worse alternatives).
	 * 3. Sets relativized EcoScore values.
	 *
	 * <p>In methodology V2 (design GOU-1878, 2.2 and 2.4), missing criteria are not imputed: the weights are
	 * redistributed over the real criteria, a product below the coverage threshold gets no ImpactScore and a fresh
	 * {@link EcoScoreRanking}, and only rated products are ranked, ties sharing a rank.
	 * 
	 * @param datas Collection of products processed
	 * @param vConf Vertical configuration
	 */
	@Override
	public void done(Collection<Product> datas, VerticalConfig vConf) {

		LOGGER.info("EcoScore done() start. Products: {}", datas.size());

		if (vConf.getImpactScoreConfig() == null || vConf.getImpactScoreConfig().activeCriteriasPonderation().isEmpty()) {
			LOGGER.error("No ImpactScore defined for vertical {}", vConf.getId());
			return;
		}

		Map<String, Double> normalizedWeights = normalizeWeights(vConf);
		if (normalizedWeights.isEmpty()) {
			LOGGER.error("EcoScore weights sum to zero for vertical {}", vConf.getId());
			return;
		}

		boolean v2 = isMethodologyV2(vConf);
		if (!v2) {
			Set<String> missingScoresInBatch = resolveMissingScoresInBatch(datas, vConf);
			missingScoresInBatch.forEach(scoreName ->
					LOGGER.warn("EcoScore criteria '{}' is missing in batch. Applying missing-value policy.", scoreName));
		}

		// Compute the ecoscore from existing scores
		for (Product data : datas) {
			try {
				Double score = v2 ? generateV2EcoScore(data, vConf) : generateEcoScore(data, vConf, normalizedWeights);
				if (score == null) {
					if (v2) {
						// Not rated: no ImpactScore at all, the real sub-scores stay
						data.getScores().remove(ECOSCORE_SCORENAME);
					} else {
						LOGGER.warn("EcoScore rating skipped for {} due to missing sub-scores", data.getId());
					}
					continue;
				}

				// Processing cardinality
				incrementCardinality(ECOSCORE_SCORENAME, score, vConf);

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

		// Assessment metadata, on every product of the cohort, rated or not (design GOU-1878, 1.1)
		long scoredAt = System.currentTimeMillis();
		for (Product data : datas) {
			data.setImpactScoreMeta(assess(data, vConf, scoredAt));
			if (v2) {
				// Rebuilt below for rated products; an unrated product keeps no position
				data.setRanking(new EcoScoreRanking());
			}
		}

		List<Product> productsWithRealEcoScore = datas.stream().filter(this::hasRealEcoScore).toList();
		LOGGER.info("Products with real EcoScore: {}", productsWithRealEcoScore.size());

		if (productsWithRealEcoScore.isEmpty()) {
			LOGGER.info("{} -> No real ecoscore computed, skipping ranking", this.getClass().getSimpleName());
			return;
		}

		try {
			// EcoScore absolute stays on 0-20 scale; relativ is scaled to 0-5
			// so that Score.on20() and Score.percent() produce correct values.
			double relativScale = StandardiserService.DEFAULT_MAX_RATING / ECOSCORE_TARGET_SCALE;
			for (Product product : productsWithRealEcoScore) {
				Cardinality absolute = product.ecoscore().getAbsolute();
				Cardinality relativ = new Cardinality(absolute);
				relativ.setValue(absolute.getValue() * relativScale);
				relativ.setMin(absolute.getMin() != null ? absolute.getMin() * relativScale : null);
				relativ.setMax(absolute.getMax() != null ? absolute.getMax() * relativScale : null);
				relativ.setAvg(absolute.getAvg() != null ? absolute.getAvg() * relativScale : null);
				product.ecoscore().setRelativ(relativ);
				product.ecoscore().setValue(absolute.getValue());
			}
	
			///////////////////////
			// EcoScore ranking and "best alternativ" reach
			///////////////////////
			List<Product> sorted = new ArrayList<>(productsWithRealEcoScore);
			// Ties are broken by GTIN, so that two products with the same ImpactScore keep their position whatever the
			// order in which the batch received them.
			sorted.sort(Comparator.<Product>comparingDouble(p -> p.ecoscore().getRelativ().getValue())
					.thenComparing(Product::getId, Comparator.nullsFirst(Comparator.naturalOrder())));
	
			int count = sorted.size();
			Long bestProductId = sorted.get(count - 1).getId();

			if (v2) {
				rankSharingTies(sorted, bestProductId);
				LOGGER.info("EcoScore done() finished.");
				return;
			}
	
			for (int i = 0; i < count; i++) {
				Product product = sorted.get(i);
				EcoScoreRanking ranking = ensureRanking(product);
				ranking.setGlobalCount(count);
				ranking.setGlobalPosition(count - i);
				ranking.setGlobalBest(bestProductId);

				// The ImpactScore rank follows the same order as globalPosition. The rank set by the parent class was
				// computed on the normalized value, where clamped extreme products tie (GOU-1894).
				Score impactScore = product.ecoscore();
				impactScore.setRanking(i);
				impactScore.getAbsolute().setCount(count);
				impactScore.getRelativ().setCount(count);
	
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

	/**
	 * V2 ranking of the rated products (design GOU-1878, 2.4): {@code count} and {@code globalCount} are the number
	 * of rated products, and tied products share a rank. {@code scores.ECOSCORE.ranking} is the number of rated
	 * products strictly worse, {@code globalPosition} 1 plus the number of rated products strictly better, and
	 * {@code globalBetter} the lowest strictly better product.
	 *
	 * @param sorted        the rated products, worst first, ties ordered by GTIN
	 * @param bestProductId the best rated product
	 */
	private void rankSharingTies(List<Product> sorted, Long bestProductId) {
		int count = sorted.size();
		int tieStart = 0;
		while (tieStart < count) {
			double value = sorted.get(tieStart).ecoscore().getRelativ().getValue();
			int tieEnd = tieStart;
			while (tieEnd < count && Double.compare(sorted.get(tieEnd).ecoscore().getRelativ().getValue(), value) == 0) {
				tieEnd++;
			}
			// tieStart products are strictly worse, count - tieEnd strictly better
			for (int i = tieStart; i < tieEnd; i++) {
				Product product = sorted.get(i);
				EcoScoreRanking ranking = ensureRanking(product);
				ranking.setGlobalCount(count);
				ranking.setGlobalPosition(1L + count - tieEnd);
				ranking.setGlobalBest(bestProductId);
				ranking.setGlobalBetter(tieEnd < count ? sorted.get(tieEnd).getId() : null);

				Score impactScore = product.ecoscore();
				impactScore.setRanking(tieStart);
				impactScore.getAbsolute().setCount(count);
				impactScore.getRelativ().setCount(count);
			}
			tieStart = tieEnd;
		}
	}

	/**
	 * Recomputes the ImpactScore of a product from the sub-scores it already holds, outside a batch.
	 *
	 * <p>Used for products a batch does not rescore (no offers) whose stored ImpactScore is wrong. Their
	 * sub-scores keep the relative values of the batch that produced them, so the result is the score that batch
	 * should have stored. The ranking is restored from {@link EcoScoreRanking}, which that batch computed on the
	 * weighted score.
	 *
	 * <p>Limited to the lot 0 defect of methodology V1 (composite ImpactScore). In V2 a product the batch does not
	 * rescore is never recomputed (design GOU-1878, 2.5), so this method returns {@code null}.
	 *
	 * @param product the product holding the sub-scores
	 * @param vConf   the vertical configuration defining the weights
	 * @return the ImpactScore, or {@code null} in V2, when the vertical has no weights or a sub-score cannot be read
	 */
	public Score recomputeFromStoredSubScores(Product product, VerticalConfig vConf) {
		if (vConf == null || vConf.getImpactScoreConfig() == null || isMethodologyV2(vConf)) {
			return null;
		}
		Map<String, Double> normalizedWeights = normalizeWeights(vConf);
		if (normalizedWeights.isEmpty()) {
			return null;
		}
		Double value;
		try {
			value = generateEcoScore(product, vConf, normalizedWeights);
		} catch (ValidationException e) {
			LOGGER.warn("ImpactScore recomputation failed for {} : {}", product.getId(), e.getMessage());
			return null;
		}
		if (value == null) {
			return null;
		}

		Score score = new Score(ECOSCORE_SCORENAME, value);
		Cardinality absolute = new Cardinality();
		absolute.setValue(value);
		Cardinality relativ = new Cardinality();
		relativ.setValue(value * StandardiserService.DEFAULT_MAX_RATING / ECOSCORE_TARGET_SCALE);

		EcoScoreRanking ranking = product.getRanking();
		if (ranking != null && ranking.getGlobalCount() > 0 && ranking.getGlobalPosition() > 0
				&& ranking.getGlobalPosition() <= ranking.getGlobalCount()) {
			absolute.setCount((int) ranking.getGlobalCount());
			relativ.setCount((int) ranking.getGlobalCount());
			score.setRanking((int) (ranking.getGlobalCount() - ranking.getGlobalPosition()));
		}
		score.setAbsolute(absolute);
		score.setRelativ(relativ);
		return score;
	}

	/**
	 * Builds the assessment metadata of a product of the scored cohort: version and revision of the active
	 * methodology, batch instant, coverage C of the active matrix and outcome.
	 *
	 * <p>A criterion is real when the product holds its score, not virtual, with a readable relative value. C is the
	 * weight of the real criteria over the weight of the criteria weighted above 0. In V1 C is informative: missing
	 * criteria are imputed and the product is {@code RATED} whenever it holds a real ImpactScore; otherwise it is
	 * {@code NOT_RATED}, with {@code NO_REAL_CRITERIA} when no criterion is real and no reason else (V1 has no
	 * threshold). In V2 C decides: {@code NOT_RATED} with {@code NO_REAL_CRITERIA} when C is 0, {@code LOW_COVERAGE}
	 * when C is below {@code minRealWeightCoverage}.
	 *
	 * @param product  a product of the cohort, after the ImpactScore computation
	 * @param vConf    the vertical configuration
	 * @param scoredAt instant of the batch, epoch milliseconds
	 * @return the metadata to store on the product
	 */
	ImpactScoreMeta assess(Product product, VerticalConfig vConf, long scoredAt) {
		ImpactScoreConfig impactScoreConfig = vConf.getImpactScoreConfig();
		Coverage coverage = coverage(product, impactScoreConfig);

		ImpactScoreMeta meta = new ImpactScoreMeta();
		meta.setMethodologyVersion(impactScoreConfig.effectiveMethodologyVersion());
		meta.setMethodologyRevision(impactScoreConfig.getMethodologyRevision());
		meta.setScoredAt(scoredAt);
		meta.setRealWeightCoverage(coverage.ratio());
		meta.setRealCriteriaCount(coverage.realCount());
		meta.setTotalCriteriaCount(coverage.totalCount());
		meta.setRealCriteria(coverage.realCriteria());
		if (hasRealEcoScore(product)) {
			meta.setStatus(ImpactScoreMeta.Status.RATED.name());
		} else {
			meta.setStatus(ImpactScoreMeta.Status.NOT_RATED.name());
			ImpactScoreMeta.NotRatedReason reason;
			if (isMethodologyV2(vConf)) {
				reason = v2NotRatedReason(coverage, impactScoreConfig.minRealWeightCoverage());
			} else {
				reason = coverage.realCount() == 0 ? ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA : null;
			}
			meta.setNotRatedReason(reason == null ? null : reason.name());
		}
		return meta;
	}

	/**
	 * Coverage of the active matrix by the real criteria of a product: present, not virtual, with a readable
	 * relative value. Only criteria weighted above 0 count.
	 *
	 * @param product           the product holding the sub-scores
	 * @param impactScoreConfig the vertical's ImpactScore configuration
	 * @return the coverage
	 */
	Coverage coverage(Product product, ImpactScoreConfig impactScoreConfig) {
		double totalWeight = 0.0;
		double realWeight = 0.0;
		double weightedRelativeSum = 0.0;
		int totalCount = 0;
		int realCount = 0;
		List<String> realCriteria = new ArrayList<>();
		for (Map.Entry<String, Double> weight : impactScoreConfig.activeCriteriasPonderation().entrySet()) {
			if (weight.getValue() == null || weight.getValue() <= 0.0) {
				continue;
			}
			totalWeight += weight.getValue();
			totalCount++;
			Score score = product.getScores().get(weight.getKey());
			if (isRealScore(weight.getKey(), score)) {
				realWeight += weight.getValue();
				realCount++;
				realCriteria.add(weight.getKey());
				weightedRelativeSum += weight.getValue() * resolveRelativeValue(weight.getKey(), score);
			}
		}
		return new Coverage(realWeight, totalWeight, realCount, totalCount, weightedRelativeSum, List.copyOf(realCriteria));
	}

	/**
	 * Why a product is not rated in V2 (design GOU-1878, 2.2), by the rule the exclusion shares
	 * ({@link ImpactScoreCoverage#notRatedReason(double, double, Double)}). The threshold is inclusive, within
	 * {@link #COVERAGE_TOLERANCE}.
	 *
	 * @param coverage  the coverage of the product
	 * @param threshold {@code minRealWeightCoverage}, {@code null} for no threshold
	 * @return {@code NO_REAL_CRITERIA} when C is 0, {@code LOW_COVERAGE} when C is below the threshold, {@code null}
	 *         when the product is rated
	 */
	static ImpactScoreMeta.NotRatedReason v2NotRatedReason(Coverage coverage, Double threshold) {
		if (coverage.realCount() == 0) {
			return ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA;
		}
		return ImpactScoreCoverage.notRatedReason(coverage.realWeight(), coverage.totalWeight(), threshold);
	}

	private Map<String, Double> normalizeWeights(VerticalConfig vConf) {
		Map<String, Double> weights = vConf.getImpactScoreConfig().activeCriteriasPonderation();
		if (weights == null || weights.isEmpty()) {
			return Map.of();
		}
		double sum = weights.values().stream()
				.filter(value -> value != null)
				.mapToDouble(Double::doubleValue)
				.sum();
		if (sum == 0.0) {
			return Map.of();
		}
		double factor = (ECOSCORE_TARGET_SCALE / StandardiserService.DEFAULT_MAX_RATING) / sum;
		Map<String, Double> normalized = new HashMap<>();
		weights.forEach((key, value) -> {
			if (value != null) {
				normalized.put(key, value * factor);
			}
		});
		return normalized;
	}

	private Set<String> resolveMissingScoresInBatch(Collection<Product> datas, VerticalConfig vConf) {
		if (vConf == null || vConf.getImpactScoreConfig() == null || datas == null) {
			return Set.of();
		}
		return vConf.getImpactScoreConfig().activeCriteriasPonderation().keySet().stream()
				.filter(scoreName -> datas.stream().noneMatch(product -> product.getScores().containsKey(scoreName)))
				.collect(java.util.stream.Collectors.toSet());
	}

	private ScoreMissingValuePolicy resolveMissingValuePolicy(String config, VerticalConfig vConf) {
		AttributeConfig attributeConfig = vConf == null || vConf.getAttributesConfig() == null
				? null
				: vConf.getAttributesConfig().getAttributeConfigByKey(config);
		if (attributeConfig == null || attributeConfig.getScoring() == null) {
			return ScoreMissingValuePolicy.WORST;
		}
		return attributeConfig.getScoring().getMissingValuePolicy();
	}

	private Double resolveMissingScoreFallback(String config, VerticalConfig vConf, ScoreMissingValuePolicy policy) {
		AttributeConfig attributeConfig = vConf == null || vConf.getAttributesConfig() == null
				? null
				: vConf.getAttributesConfig().getAttributeConfigByKey(config);
		if (attributeConfig == null || attributeConfig.getScoring() == null || attributeConfig.getScoring().getScale() == null) {
			return ScoreMissingValuePolicy.WORST.equals(policy) ? 0.0 : StandardiserService.DEFAULT_MAX_RATING / 2.0;
		}
		Double min = attributeConfig.getScoring().getScale().getMin();
		Double max = attributeConfig.getScoring().getScale().getMax();
		if (min == null || max == null) {
			return ScoreMissingValuePolicy.WORST.equals(policy) ? 0.0 : StandardiserService.DEFAULT_MAX_RATING / 2.0;
		}
		if (ScoreMissingValuePolicy.WORST.equals(policy)) {
			return min;
		}
		return (min + max) / 2.0;
	}

	private EcoScoreRanking ensureRanking(Product product) {
		EcoScoreRanking ranking = product.getRanking();
		if (ranking == null) {
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
