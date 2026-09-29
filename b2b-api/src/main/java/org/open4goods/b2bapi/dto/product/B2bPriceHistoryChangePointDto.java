package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import org.open4goods.pricehistory.model.OfferAvailability;

/**
 * One sparse price-change point (CHANGE granularity).
 *
 * @param time observation instant
 * @param amount advertised amount at this point
 * @param state advertised availability state at this point
 */
@Schema(description = "One sparse price-change point (CHANGE granularity).")
public record B2bPriceHistoryChangePointDto(
        @Schema(description = "Observation instant.", example = "2026-06-15T09:30:00Z")
        Instant time,
        @Schema(description = "Advertised amount at this point.", example = "799.99")
        BigDecimal amount,
        @Schema(description = "Advertised availability state at this point.", example = "AVAILABLE")
        OfferAvailability state) {
}
