package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.model.price.Currency;
import org.open4goods.model.product.ProductCondition;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Offer of a {@link ProductCardDto}: the subset of {@link ProductAggregatedPriceDto} rendered by the price rows of a
 * card (merchant, price and redirection link).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardOfferDto(
        @Schema(description = "Datasource providing the offer", example = "amazon.fr", nullable = true)
        String datasourceName,
        @Schema(description = "URL pointing to the datasource favicon", format = "uri", nullable = true)
        String favicon,
        @Schema(description = "Target URL of the offer", nullable = true)
        String url,
        @Schema(description = "Condition of the product for this offer", nullable = true)
        ProductCondition condition,
        @Schema(description = "Affiliation token when available", nullable = true)
        String affiliationToken,
        @Schema(description = "Price value", example = "329.98", nullable = true)
        Double price,
        @Schema(description = "Currency of the price", nullable = true)
        Currency currency,
        @Schema(description = "Human friendly representation of the price", example = "330", nullable = true)
        String shortPrice,
        @Schema(description = "Available stock quantity reported by the merchant", example = "12", nullable = true)
        Integer quantityInStock
) {
}
