package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregated view of price history for a specific product condition.
 */
public record ProductPriceHistoryDto(
        @Schema(description = "Full history entries sorted chronologically")
        List<ProductPriceHistoryEntryDto> entries,
        @Schema(description = "Lowest recorded price in history", nullable = true)
        ProductPriceHistoryEntryDto lowest,
        @Schema(description = "Highest recorded price in history", nullable = true)
        ProductPriceHistoryEntryDto highest,
        @Schema(description = "Average price over the recorded history", nullable = true)
        Double average
) {
}
