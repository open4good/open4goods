package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Currency;
import org.open4goods.model.product.ProductCondition;

/**
 * Sanitized historical price summary for one product condition.
 *
 * @param condition product condition summarized
 * @param lowestAmount lowest historical amount
 * @param lowestAt timestamp of the lowest historical amount
 * @param highestAmount highest historical amount
 * @param highestAt timestamp of the highest historical amount
 * @param averageAmount average historical amount
 * @param currency response currency
 */
@Schema(description = "Sanitized historical price summary for one product condition.")
public record B2bPriceHistorySummaryDto(
        @Schema(description = "Product condition summarized.", example = "NEW")
        ProductCondition condition,
        @Schema(description = "Lowest historical amount.", example = "749.99", nullable = true)
        Double lowestAmount,
        @Schema(description = "Timestamp of the lowest historical amount.", example = "2026-05-20T08:00:00Z", nullable = true)
        Instant lowestAt,
        @Schema(description = "Highest historical amount.", example = "899.99", nullable = true)
        Double highestAmount,
        @Schema(description = "Timestamp of the highest historical amount.", example = "2026-04-12T08:00:00Z", nullable = true)
        Instant highestAt,
        @Schema(description = "Average historical amount.", example = "812.49", nullable = true)
        Double averageAmount,
        @Schema(description = "Response currency.", example = "EUR")
        Currency currency) {
}
