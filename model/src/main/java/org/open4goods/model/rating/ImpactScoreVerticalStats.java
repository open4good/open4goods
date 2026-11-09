package org.open4goods.model.rating;

import java.util.List;
import java.util.Map;

/**
 * Measured figures of the ImpactScore of one vertical, read from the products' assessment metadata
 * ({@code impactScoreMeta}) in a single aggregated query.
 *
 * <p>Only products of the vertical's current methodology, with an offer and not excluded, are counted. A figure
 * that cannot be measured is {@code null} (or an empty map), never an estimate.
 *
 * @param ratedCount                 products holding an ImpactScore
 * @param notRatedLowCoverageCount   products not rated for lack of criteria weight (LOW_COVERAGE)
 * @param notRatedNoCriteriaCount    products not rated because no criterion is filled in
 * @param outdatedCount              products with offers still assessed with another methodology version, waiting for the next recalculation
 * @param excludedCount              products of the vertical excluded from the catalogue
 * @param coverageByCriterion        rated products having each criterion filled in (not imputed), by criterion key
 * @param realCriteriaCountDistribution rated products by number of criteria filled in
 * @param scoreHistogram             rated products per score band of {@link #scoreBandWidth()} out of 20, from 0
 * @param scoreBandWidth             width of a band of the histogram, on a 0 to 20 scale
 * @param scoreMin                   lowest ImpactScore out of 20, {@code null} when none
 * @param scoreP5                    5th percentile
 * @param scoreMedian                median
 * @param scoreP95                   95th percentile
 * @param scoreMax                   highest ImpactScore out of 20
 * @param lastScoredAt               most recent batch instant, epoch milliseconds, {@code null} when none
 */
public record ImpactScoreVerticalStats(
		long ratedCount,
		long notRatedLowCoverageCount,
		long notRatedNoCriteriaCount,
		long outdatedCount,
		long excludedCount,
		Map<String, Long> coverageByCriterion,
		Map<Integer, Long> realCriteriaCountDistribution,
		List<Long> scoreHistogram,
		int scoreBandWidth,
		Double scoreMin,
		Double scoreP5,
		Double scoreMedian,
		Double scoreP95,
		Double scoreMax,
		Long lastScoredAt) {
}
