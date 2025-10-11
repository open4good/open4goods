package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Entry representing a price history point.
 */
public record ProductPriceHistoryEntryDto(
        @Schema(description = "Timestamp of the history entry in epoch milliseconds")
        Long timestamp,
        @Schema(description = "Price recorded at the given timestamp")
        Double price
) {
}
