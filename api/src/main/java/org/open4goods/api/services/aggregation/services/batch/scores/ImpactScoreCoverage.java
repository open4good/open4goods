package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Coverage C of the active ImpactScore matrix (design {@code docs/impactscore/v2-design.md}, sections 2.2 and 3):
 * the weight of the real criteria over the weight of the criteria weighted above 0, and the V2 rating rule on it.
 *
 * <p>The batch ({@link EcoScoreAggregationService}) counts as real the sub-scores it computed. The exclusion
 * ({@code ExclusionRules}, GOU-2165) runs before any score exists and must not read the scores of an earlier batch:
 * an excluded product loses its scores, so it could never come back. It counts as real the criteria whose value the
 * scoring can read from the indexed attributes, with the same pure functions as {@link
 * Attribute2ScoreAggregationService}: same attributes, same result, in realtime and in the batch.
 */
public final class ImpactScoreCoverage {

	/** Tolerance of the V2 coverage threshold: a coverage C is rated when {@code C >= threshold - 1e-9}. */
	public static final double TOLERANCE = 1e-9;

	private ImpactScoreCoverage() {
	}

	/**
	 * Why a product is not rated in V2 (design GOU-1878, 2.2). The threshold is inclusive, within
	 * {@link #TOLERANCE}.
	 *
	 * @param realWeight  sum of the weights of the real criteria
	 * @param totalWeight sum of the weights of the criteria weighted above 0
	 * @param threshold   {@code minRealWeightCoverage}, {@code null} for no threshold
	 * @return {@code NO_REAL_CRITERIA} when C is 0, {@code LOW_COVERAGE} when C is below the threshold, {@code null}
	 *         when the product is rated
	 */
	public static ImpactScoreMeta.NotRatedReason notRatedReason(double realWeight, double totalWeight, Double threshold) {
		if (realWeight <= 0.0 || totalWeight <= 0.0) {
			return ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA;
		}
		if (threshold != null && realWeight / totalWeight < threshold - TOLERANCE) {
			return ImpactScoreMeta.NotRatedReason.LOW_COVERAGE;
		}
		return null;
	}

	/**
	 * Why a product would not be rated, from its indexed attributes only: the V2 rule of
	 * {@link #notRatedReason(double, double, Double)} on the active matrix and threshold, with
	 * {@link #criteriaWithValue(Product, VerticalConfig)} as real criteria.
	 *
	 * @param product the product, with its indexed attributes
	 * @param vConf   the vertical configuration
	 * @return the reason, {@code null} when the product reaches the threshold
	 */
	public static ImpactScoreMeta.NotRatedReason notRatedReasonFromAttributes(Product product, VerticalConfig vConf) {
		ImpactScoreConfig impactScoreConfig = vConf.getImpactScoreConfig();
		if (impactScoreConfig == null) {
			return ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA;
		}
		Set<String> real = criteriaWithValue(product, vConf);
		double totalWeight = 0.0;
		double realWeight = 0.0;
		for (Map.Entry<String, Double> weight : impactScoreConfig.activeCriteriasPonderation().entrySet()) {
			if (weight.getValue() == null || weight.getValue() <= 0.0) {
				continue;
			}
			totalWeight += weight.getValue();
			if (real.contains(weight.getKey())) {
				realWeight += weight.getValue();
			}
		}
		return notRatedReason(realWeight, totalWeight, impactScoreConfig.minRealWeightCoverage());
	}

	/**
	 * The criteria of the active matrix, weighted above 0, whose value the scoring can read from the product's
	 * indexed attributes: an {@code asScore} attribute with a numeric or mapped value its transform accepts, or a
	 * derived attribute whose sources hold what its method needs. A criterion that is not an {@code asScore}
	 * attribute never counts (a V2 matrix has none, design 1.2).
	 *
	 * @param product the product, with its indexed attributes
	 * @param vConf   the vertical configuration
	 * @return the criteria, in the order of the matrix
	 */
	public static Set<String> criteriaWithValue(Product product, VerticalConfig vConf) {
		Set<String> real = new LinkedHashSet<>();
		AttributesConfig attributesConfig = vConf.getAttributesConfig();
		if (attributesConfig == null || vConf.getImpactScoreConfig() == null) {
			return real;
		}
		Map<String, IndexedAttribute> attributesByKey = Attribute2ScoreAggregationService.attributesByKey(product,
				attributesConfig);
		for (Map.Entry<String, Double> weight : vConf.getImpactScoreConfig().activeCriteriasPonderation().entrySet()) {
			if (weight.getValue() == null || weight.getValue() <= 0.0) {
				continue;
			}
			AttributeConfig attribute = attributesConfig.getAttributeConfigByKey(weight.getKey());
			if (attribute != null && attribute.isAsScore() && hasValue(attribute, attributesByKey, attributesConfig)) {
				real.add(weight.getKey());
			}
		}
		return real;
	}

	private static boolean hasValue(AttributeConfig attribute, Map<String, IndexedAttribute> attributesByKey,
			AttributesConfig attributesConfig) {
		try {
			Double raw;
			if (attribute.isDerivedScore()) {
				Attribute2ScoreAggregationService.DerivedValue value = Attribute2ScoreAggregationService
						.derivedValue(attribute, attributesByKey, attributesConfig);
				raw = value == null ? null : value.raw();
			} else {
				IndexedAttribute aga = attributesByKey.get(attribute.getKey());
				raw = aga == null ? null : Attribute2ScoreAggregationService.rawValue(attribute.getKey(), aga, attributesConfig);
			}
			return raw != null && Attribute2ScoreAggregationService.applyTransform(raw, attribute) != null;
		} catch (ValidationException | RuntimeException e) {
			// The scoring cannot read it either
			return false;
		}
	}
}
