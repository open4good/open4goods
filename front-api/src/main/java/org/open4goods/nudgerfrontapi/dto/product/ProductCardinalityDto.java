package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cardinality statistics associated with a score.
 */
public record ProductCardinalityDto(
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
        @Schema(description = "Standard deviation of the distribution")
        Double stdDev,
        @Schema(description = "Current value")
        Double value
) {
}
