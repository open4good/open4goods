package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API representation of a product price trend, exposing the direction of the
 * change and additional pricing metrics extracted from the history.
 */
public record ProductPriceTrendDto(
        @Schema(description = "Direction of the price evolution compared to the previous entry", implementation = PriceTrendState.class, nullable = true)
        PriceTrendState trend,
        @Schema(description = "Elapsed time in milliseconds between the two last price measurements", example = "86400000", nullable = true)
        Long period,
        @Schema(description = "Current price used for the trend computation", example = "199.99", nullable = true)
        Double actualPrice,
        @Schema(description = "Previous price used as a comparison point", example = "189.99", nullable = true)
        Double lastPrice,
        @Schema(description = "Absolute price difference between the current and the previous value", example = "10.0", nullable = true)
        Double variation,
        @Schema(description = "Lowest historical price found in the series", example = "149.99", nullable = true)
        Double historicalLowestPrice,
        @Schema(description = "Difference between the current price and the historical lowest value", example = "50.0", nullable = true)
        Double historicalVariation
) {
}
