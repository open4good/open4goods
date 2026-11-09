package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Measured figures of the ImpactScore of one vertical, for its method page: counts by outcome, real coverage per
 * criterion and distribution of the scores. Taken from the batch's assessment of each product, cached; a figure
 * that is not measured is null or empty, never estimated.
 */
public record ImpactScoreStatsDto(
        @Schema(description = "Vertical identifier", example = "tv")
        String verticalId,
        @Schema(description = "Methodology version the figures relate to", example = "1")
        int methodologyVersion,
        @Schema(description = "Products holding an ImpactScore")
        long ratedCount,
        @Schema(description = "Products not rated for lack of criteria weight")
        long notRatedLowCoverageCount,
        @Schema(description = "Products not rated because no criterion is filled in")
        long notRatedNoCriteriaCount,
        @Schema(description = "Products with offers waiting for their recalculation with the current methodology")
        long outdatedCount,
        @Schema(description = "Products of the vertical excluded from the catalogue")
        long excludedCount,
        @Schema(description = "Rated products having each criterion filled in, by criterion key; empty when not measured yet")
        Map<String, Long> coverageByCriterion,
        @Schema(description = "Rated products by number of criteria filled in; empty when not measured yet")
        Map<Integer, Long> realCriteriaCountDistribution,
        @Schema(description = "Rated products per score band, from 0; empty when no product is rated")
        List<Long> scoreHistogram,
        @Schema(description = "Width of a histogram band, on the 0 to 20 scale", example = "2")
        int scoreBandWidth,
        @Schema(description = "Lowest score out of 20", nullable = true)
        Double scoreMin,
        @Schema(description = "5th percentile of the scores out of 20", nullable = true)
        Double scoreP5,
        @Schema(description = "Median score out of 20", nullable = true)
        Double scoreMedian,
        @Schema(description = "95th percentile of the scores out of 20", nullable = true)
        Double scoreP95,
        @Schema(description = "Highest score out of 20", nullable = true)
        Double scoreMax,
        @Schema(description = "Most recent calculation of the vertical, epoch milliseconds", nullable = true)
        Long lastScoredAt
) {
}
