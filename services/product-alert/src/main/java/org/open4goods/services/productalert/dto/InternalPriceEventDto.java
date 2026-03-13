package org.open4goods.services.productalert.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import org.open4goods.model.product.ProductCondition;

/**
 * Internal price-drop event received from the API service.
 *
 * @param gtin numeric GTIN
 * @param condition product condition
 * @param previousPrice previous best price
 * @param currentPrice current best price
 * @param eventTimestamp event timestamp
 */
public record InternalPriceEventDto(
        @NotNull(message = "gtin is required")
        Long gtin,
        @NotNull(message = "condition is required")
        ProductCondition condition,
        @NotNull(message = "previousPrice is required")
        @Positive(message = "previousPrice must be positive")
        Double previousPrice,
        @NotNull(message = "currentPrice is required")
        @Positive(message = "currentPrice must be positive")
        Double currentPrice,
        @NotNull(message = "eventTimestamp is required")
        Instant eventTimestamp)
{
}
