package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ImpactScore status of a product: whether it is rated under the vertical's current methodology, why not, and the
 * data behind the score (coverage, position, date, methodology). Design: {@code docs/impactscore/v2-design.md},
 * sections 4.1 and 4.2.
 */
public record ProductImpactScoreStatusDto(
        @Schema(description = "RATED when the product holds an ImpactScore of the vertical's current methodology, NOT_RATED otherwise",
                example = "RATED", allowableValues = {"RATED", "NOT_RATED"})
        String status,
        @Schema(description = "Why the product is not rated: LOW_COVERAGE (not enough criteria filled in), NO_REAL_CRITERIA (none filled in), OUTDATED_METHOD (score of another methodology version), NOT_SCORED (never assessed)",
                example = "OUTDATED_METHOD", allowableValues = {"LOW_COVERAGE", "NO_REAL_CRITERIA", "OUTDATED_METHOD", "NOT_SCORED"},
                nullable = true)
        String notRatedReason,
        @Schema(description = "Methodology version of the last assessment; null when never assessed with a recorded version (legacy score, treated as version 1)",
                example = "1", nullable = true)
        Integer methodologyVersion,
        @Schema(description = "Fingerprint of the ImpactScore matrix of the last assessment", example = "3f9a1c2e", nullable = true)
        String methodologyRevision,
        @Schema(description = "Instant of the last assessment, epoch milliseconds", example = "1794163200000", nullable = true)
        Long scoredAt,
        @Schema(description = "Current methodology version of the vertical", example = "1")
        int currentMethodologyVersion,
        @Schema(description = "Weight of the criteria filled in over the weight of the matrix, between 0 and 1", example = "0.82",
                minimum = "0", maximum = "1", nullable = true)
        Double realWeightCoverage,
        @Schema(description = "Criteria of the matrix filled in", example = "11", nullable = true)
        Integer realCriteriaCount,
        @Schema(description = "Criteria of the matrix", example = "13", nullable = true)
        Integer totalCriteriaCount,
        @Schema(description = "Position among rated products, 1 = best; null when not rated", example = "4", nullable = true)
        Long position,
        @Schema(description = "Number of rated products of the vertical; null when not rated", example = "250", nullable = true)
        Long ratedCount,
        @Schema(description = "Share of rated products ranked below this product, percent 0-100; null when not rated or alone",
                example = "97", minimum = "0", maximum = "100", nullable = true)
        Integer betterThanPercent,
        @Schema(description = "Position label key (for example excellent) from the vertical's position labels; null in methodology version 1 or when not rated",
                example = "excellent", nullable = true)
        String level
) {
}
