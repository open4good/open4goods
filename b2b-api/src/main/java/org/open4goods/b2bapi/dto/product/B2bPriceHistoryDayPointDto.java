package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One daily provider-rollup point (DAY granularity).
 *
 * @param date UTC calendar day
 * @param minAmount lowest present-offer amount observed that day
 * @param maxAmount highest present-offer amount observed that day
 * @param closeAmount last amount under observation-time then stable-key ordering
 * @param offerCount distinct offers observed present that day
 */
@Schema(description = "One daily provider-rollup point (DAY granularity).")
public record B2bPriceHistoryDayPointDto(
        @Schema(description = "UTC calendar day.", example = "2026-06-15")
        LocalDate date,
        @Schema(description = "Lowest present-offer amount observed that day.", example = "749.00")
        BigDecimal minAmount,
        @Schema(description = "Highest present-offer amount observed that day.", example = "819.99")
        BigDecimal maxAmount,
        @Schema(description = "Last amount under observation-time then stable-key ordering.", example = "799.99")
        BigDecimal closeAmount,
        @Schema(description = "Distinct offers observed present that day.", example = "3")
        long offerCount) {
}
