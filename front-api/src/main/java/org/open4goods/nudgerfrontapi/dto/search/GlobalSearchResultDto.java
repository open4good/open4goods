package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;

/**
 * Represents a single product hit returned by the global search endpoint.
 */
public record GlobalSearchResultDto(
        @Schema(description = "Full product payload returned by the search result")
        ProductDto product,
        @Schema(description = "Pertinence score returned by Elasticsearch", example = "7.42")
        double score,
        @Schema(description = "Effective search mode that produced the result", implementation = SearchMode.class)
        SearchMode searchMode) {
}
