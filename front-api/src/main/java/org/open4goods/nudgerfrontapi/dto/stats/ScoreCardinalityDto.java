package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cardinality statistics for a score distribution.
 */
public record ScoreCardinalityDto(
        @Schema(description = "Minimal value encountered")
        Double min,
        @Schema(description = "Maximal value encountered")
        Double max,
        @Schema(description = "Average value across the population")
        Double avg,
        @Schema(description = "Number of elements composing the population")
        Integer count,
        @Schema(description = "Sum of the values")
        Double sum,
        @Schema(description = "Standard deviation σ of the distribution (variance = σ²)")
        Double stdDev
) {
}
