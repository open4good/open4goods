package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API payload combining paginated products with aggregation buckets for chart rendering.
 */
public record ProductSearchResponseDto(
        @Schema(description = "Paginated products matching the search criteria")
        PageDto<ProductDto> products,
        @Schema(description = "Aggregation buckets computed for the current query")
        List<AggregationResponseDto> aggregations) {
}
