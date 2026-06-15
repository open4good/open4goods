package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import org.open4goods.model.product.ProductCondition;

/**
 * Sanitized price trend for one product condition.
 *
 * @param condition product condition tracked by the trend
 * @param direction trend direction: -1 decreasing, 0 stable, 1 increasing
 * @param actualAmount current best amount
 * @param previousAmount previous historical amount
 * @param variation absolute amount variation from the previous value
 * @param variationPercent percent variation from the previous value
 * @param historicalLowestAmount lowest historical amount used for comparison
 * @param historicalVariation variation from the historical lowest amount
 * @param periodMs elapsed time between compared points, in milliseconds
 */
@Schema(description = "Sanitized price trend for one product condition.")
public record B2bPriceTrendDto(
        @Schema(description = "Product condition tracked by the trend.", example = "NEW")
        ProductCondition condition,
        @Schema(description = "Trend direction: -1 decreasing, 0 stable, 1 increasing.", example = "-1")
        Integer direction,
        @Schema(description = "Current best amount.", example = "799.99", nullable = true)
        Double actualAmount,
        @Schema(description = "Previous historical amount.", example = "849.99", nullable = true)
        Double previousAmount,
        @Schema(description = "Absolute amount variation from the previous value.", example = "-50.0", nullable = true)
        Double variation,
        @Schema(description = "Percent variation from the previous value.", example = "-5.88", nullable = true)
        Double variationPercent,
        @Schema(description = "Lowest historical amount used for comparison.", example = "749.99", nullable = true)
        Double historicalLowestAmount,
        @Schema(description = "Variation from the historical lowest amount.", example = "50.0", nullable = true)
        Double historicalVariation,
        @Schema(description = "Elapsed time between compared points, in milliseconds.", example = "604800000", nullable = true)
        Long periodMs) {
}
