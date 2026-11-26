package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API payload combining paginated product cards with aggregation buckets, returned by {@code POST /products/cards}.
 */
public record ProductCardSearchResponseDto(
        @Schema(description = "Paginated product cards matching the search criteria")
        PageDto<ProductCardDto> products,
        @Schema(description = "Aggregation buckets computed for the current query")
        List<AggregationResponseDto> aggregations) {
}
