package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Currency;
import org.open4goods.model.product.ProductCondition;

/**
 * Public, sanitized offer payload for the Product Data API.
 *
 * @param merchant public merchant label
 * @param title offer title
 * @param url merchant offer URL
 * @param condition product condition
 * @param amount offer amount in the response currency
 * @param currency response currency
 * @param timestamp offer collection timestamp
 * @param freshnessAgeDays offer age in days at response time
 * @param faviconUrl optional merchant favicon URL
 */
@Schema(description = "Public, sanitized offer payload for the Product Data API.")
public record B2bOfferDto(
        @Schema(description = "Public merchant label. Raw datasource names are never exposed.", example = "Example Store")
        String merchant,
        @Schema(description = "Offer title.", example = "Samsung Galaxy S25 256GB - Black")
        String title,
        @Schema(description = "Merchant offer URL.", example = "https://merchant.example/products/samsung-galaxy-s25")
        String url,
        @Schema(description = "Product condition.", example = "NEW")
        ProductCondition condition,
        @Schema(description = "Offer amount in the response currency.", example = "799.99")
        Double amount,
        @Schema(description = "Response currency.", example = "EUR")
        Currency currency,
        @Schema(description = "Offer collection timestamp.", example = "2026-06-15T09:30:00Z")
        Instant timestamp,
        @Schema(description = "Offer age in days at response time.", example = "1")
        Integer freshnessAgeDays,
        @Schema(description = "Optional merchant favicon URL.", example = "https://merchant.example/favicon.ico", nullable = true)
        String faviconUrl) {
}
