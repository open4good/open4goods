package org.open4goods.api.dto.pricealert;

import java.time.Instant;
import org.open4goods.model.product.ProductCondition;

/**
 * Outbound price-drop event sent from the API service to the product-alert
 * microservice.
 *
 * @param gtin numeric GTIN
 * @param condition product condition
 * @param previousPrice previous best price
 * @param currentPrice current best price
 * @param eventTimestamp event timestamp
 */
public record InternalPriceEventDto(
        Long gtin,
        ProductCondition condition,
        Double previousPrice,
        Double currentPrice,
        Instant eventTimestamp)
{
}
