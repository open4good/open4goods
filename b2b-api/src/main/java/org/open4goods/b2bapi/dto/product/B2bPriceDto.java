package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import org.open4goods.model.product.ProductCondition;

/**
 * Public price facet payload for one product.
 *
 * @param gtin normalized GTIN requested by the client
 * @param name product name, when available
 * @param brand product brand, when available
 * @param model product model, when available
 * @param offersCount total number of sanitized offers
 * @param freshOffersCount number of offers inside the requested freshness window
 * @param bestPrice best offer across all conditions
 * @param bestNewOffer best new-condition offer
 * @param bestOccasionOffer best occasion-condition offer
 * @param offersByCondition sanitized offers grouped by condition
 * @param newTrend price trend for new-condition offers
 * @param occasionTrend price trend for occasion-condition offers
 * @param newHistorySummary historical summary for new-condition offers
 * @param occasionHistorySummary historical summary for occasion-condition offers
 */
@Schema(description = "Public price facet payload for one product.")
public record B2bPriceDto(
        @Schema(description = "Normalized GTIN requested by the client.", example = "0887276812345")
        String gtin,
        @Schema(description = "Product name, when available.", example = "Samsung Galaxy S25", nullable = true)
        String name,
        @Schema(description = "Product brand, when available.", example = "Samsung", nullable = true)
        String brand,
        @Schema(description = "Product model, when available.", example = "SM-S931B", nullable = true)
        String model,
        @Schema(description = "Total number of sanitized offers.", example = "12")
        int offersCount,
        @Schema(description = "Number of offers inside the requested freshness window.", example = "9")
        int freshOffersCount,
        @Schema(description = "Best offer across all conditions.", nullable = true)
        B2bOfferDto bestPrice,
        @Schema(description = "Best new-condition offer.", nullable = true)
        B2bOfferDto bestNewOffer,
        @Schema(description = "Best occasion-condition offer.", nullable = true)
        B2bOfferDto bestOccasionOffer,
        @Schema(description = "Sanitized offers grouped by condition.")
        Map<ProductCondition, List<B2bOfferDto>> offersByCondition,
        @Schema(description = "Price trend for new-condition offers.", nullable = true)
        B2bPriceTrendDto newTrend,
        @Schema(description = "Price trend for occasion-condition offers.", nullable = true)
        B2bPriceTrendDto occasionTrend,
        @Schema(description = "Historical summary for new-condition offers.", nullable = true)
        B2bPriceHistorySummaryDto newHistorySummary,
        @Schema(description = "Historical summary for occasion-condition offers.", nullable = true)
        B2bPriceHistorySummaryDto occasionHistorySummary) {
}
