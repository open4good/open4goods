package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.model.price.Currency;
import org.open4goods.model.product.ProductCondition;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representation of an aggregated offer price.
 */
public record ProductAggregatedPriceDto(
        @Schema(description = "Datasource providing the offer", example = "amazon.fr")
        String datasourceName,
        @Schema(description = "Displayed offer title")
        String offerName,
        @Schema(description = "Target URL of the offer", example = "https://example.org/product/123")
        String url,
        @Schema(description = "Compensation paid when the offer is converted", example = "0.5")
        Double compensation,
        @Schema(description = "Condition of the product for this offer")
        ProductCondition condition,
        @Schema(description = "Affiliation token when available")
        String affiliationToken,
        @Schema(description = "Price value")
        Double price,
        @Schema(description = "Currency of the price")
        Currency currency,
        @Schema(description = "Timestamp of the price in epoch milliseconds")
        Long timeStamp,
        @Schema(description = "Human friendly representation of the price", nullable = true)
        String shortPrice
) {
}
