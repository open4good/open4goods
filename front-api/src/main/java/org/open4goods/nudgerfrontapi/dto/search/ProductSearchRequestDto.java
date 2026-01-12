package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload accepted by the {@code POST /products} endpoint to fine-tune the
 * search behaviour.
 */
@Schema(description = "Search customisation options applied when listing products.")
public record ProductSearchRequestDto(
        @Schema(description = "Sort criteria applied to the result set.")
        SortRequestDto sort,
        @Schema(description = "Aggregations computed alongside the result set.")
        AggregationRequestDto aggs,
        @Schema(description = "Filter clauses restricting the result set.")
        FilterRequestDto filters,
        @Schema(description = "Enable semantic search for text queries.")
        Boolean semanticSearch) {
}
