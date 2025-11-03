package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload combining category and product matches for the suggest endpoint.
 */
public record SearchSuggestResponseDto(
        @ArraySchema(schema = @Schema(implementation = SearchSuggestCategoryDto.class),
                arraySchema = @Schema(description = "Category matches resolved from the in-memory vertical index."))
        List<SearchSuggestCategoryDto> categoryMatches,

        @ArraySchema(schema = @Schema(implementation = SearchSuggestProductDto.class),
                arraySchema = @Schema(description = "Product matches obtained from the classical Elasticsearch search."))
        List<SearchSuggestProductDto> productMatches
) {
}
