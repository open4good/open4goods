package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;
import java.util.Map;

import org.open4goods.model.product.ProductCondition;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Offers facet of a {@link ProductCardDto}: offer count, best offers, offers per condition (alternatives menu of
 * the price rows) and price trends. Price histories stay on the product page.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardOffersDto(
        @Schema(description = "Number of fresh offers", example = "3", nullable = true)
        Integer offersCount,
        @Schema(description = "Best offer across conditions", nullable = true)
        ProductCardOfferDto bestPrice,
        @Schema(description = "Best new offer", nullable = true)
        ProductCardOfferDto bestNewOffer,
        @Schema(description = "Best second-hand offer", nullable = true)
        ProductCardOfferDto bestOccasionOffer,
        @Schema(description = "Fresh offers grouped by condition")
        Map<ProductCondition, List<ProductCardOfferDto>> offersByCondition,
        @Schema(description = "Price trend of new offers", nullable = true)
        ProductPriceTrendDto newTrend,
        @Schema(description = "Price trend of second-hand offers", nullable = true)
        ProductPriceTrendDto occasionTrend
) {
}
