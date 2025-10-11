package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.product.ProductCondition;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pricing and offer information for a product.
 */
public record ProductOffersDto(
        @Schema(description = "Number of offers aggregated for this product", example = "12")
        Integer offersCount,
        @Schema(description = "Whether the catalogue exposes occasion offers for this product")
        boolean hasOccasions,
        @Schema(description = "Best price across all conditions", nullable = true)
        ProductAggregatedPriceDto bestPrice,
        @Schema(description = "Best price for new offers", nullable = true)
        ProductAggregatedPriceDto bestNewOffer,
        @Schema(description = "Best price for occasion offers", nullable = true)
        ProductAggregatedPriceDto bestOccasionOffer,
        @Schema(description = "Aggregated offers grouped by product condition")
        Map<ProductCondition, List<ProductAggregatedPriceDto>> offersByCondition,
        @Schema(description = "Price history for brand new products", nullable = true)
        ProductPriceHistoryDto newHistory,
        @Schema(description = "Price history for second hand products", nullable = true)
        ProductPriceHistoryDto occasionHistory,
        @Schema(description = "Price evolution trends per condition", nullable = true)
        Map<ProductCondition, Integer> trends,
        @Schema(description = "Available product conditions")
        Set<ProductCondition> conditions,
        @Schema(description = "Gap between current best price and historical lowest price", nullable = true)
        Double historyPriceGap
) {
}
