package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.open4goods.pricehistory.model.PriceHistoryGranularity;

/**
 * Public, sanitized price-history facet payload for the Product Data API.
 *
 * @param gtin normalized GTIN echoed from the request
 * @param from effective, inclusive UTC start of the served range
 * @param to effective, exclusive UTC end of the served range
 * @param granularity effective granularity actually served (DAY or CHANGE)
 * @param series price-history series, one per distinct provider/condition/currency combination
 * @param nextCursor opaque continuation for the next page, or {@code null} when this is the last page
 */
@Schema(description = "Public, sanitized price-history facet payload for the Product Data API.")
public record B2bPriceHistoryDto(
        @Schema(description = "Normalized GTIN.", example = "0885909950805")
        String gtin,
        @Schema(description = "Effective, inclusive UTC start of the served range.", example = "2026-05-16T00:00:00Z")
        Instant from,
        @Schema(description = "Effective, exclusive UTC end of the served range.", example = "2026-06-15T00:00:00Z")
        Instant to,
        @Schema(description = "Effective granularity actually served.", example = "DAY")
        PriceHistoryGranularity granularity,
        @ArraySchema(schema = @Schema(implementation = B2bPriceHistorySeriesDto.class), arraySchema = @Schema(description = "Price-history series, one per distinct provider/condition/currency combination."))
        List<B2bPriceHistorySeriesDto> series,
        @Schema(description = "Opaque continuation for the next page, absent when this is the last page.", example = "cHJpY2UtaGlzdG9yeS1jdXJzb3I6MTAw", nullable = true)
        String nextCursor) {
}
