package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Currency;
import java.util.List;
import org.open4goods.pricehistory.model.OfferCondition;

/**
 * One sanitized price-history series for a single provider/condition/currency combination.
 *
 * <p>Only one of {@link #dayPoints()} or {@link #changePoints()} is populated, matching the
 * response-level {@code granularity}. The raw provider/source id is never exposed - only the
 * reviewed public label from {@code PublicProviderLabelRegistry}, matching the redaction rules
 * already applied to the {@code product.price} facet.
 *
 * @param provider public merchant label; a series with no reviewed label is never served
 * @param condition advertised product condition for this series
 * @param currency series currency
 * @param dayPoints ascending daily rollup points, populated when granularity is DAY
 * @param changePoints ascending sparse change points, populated when granularity is CHANGE
 */
@Schema(description = "One sanitized price-history series for a single provider/condition/currency combination.")
public record B2bPriceHistorySeriesDto(
        @Schema(description = "Public merchant label. Raw source ids are never exposed.", example = "Merchant Feed Partner")
        String provider,
        @Schema(description = "Product condition advertised for this series.", example = "NEW")
        OfferCondition condition,
        @Schema(description = "Series currency.", example = "EUR")
        Currency currency,
        @ArraySchema(schema = @Schema(implementation = B2bPriceHistoryDayPointDto.class), arraySchema = @Schema(description = "Ascending daily rollup points, populated when granularity is DAY."))
        List<B2bPriceHistoryDayPointDto> dayPoints,
        @ArraySchema(schema = @Schema(implementation = B2bPriceHistoryChangePointDto.class), arraySchema = @Schema(description = "Ascending sparse change points, populated when granularity is CHANGE."))
        List<B2bPriceHistoryChangePointDto> changePoints) {
}
