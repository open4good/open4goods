package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for global product search.
 *
 * @param query free-text query supplied by the caller
 * @param filters optional filters scoped to the global search
 * @param sort optional sort definition applied to the global search results
 */
public record GlobalSearchRequestDto(
        @Schema(description = "Free-text query used to search across products.",
                example = "Fairphone 4")
        String query,
        @Schema(description = "Optional filters applied to global search results.")
        FilterRequestDto filters,
        @Schema(description = "Optional sort definition applied to global search results.")
        SortRequestDto sort,
        @Deprecated(forRemoval = true)
        @Schema(description = "Deprecated compatibility field. Accepted but ignored; search is always lexical.",
                example = "TEXT", deprecated = true)
        String searchType,
        @Schema(description = "Zero-based page index for missing-vertical results.", example = "0")
        Integer pageNumber,
        @Schema(description = "Page size for missing-vertical results.", example = "20", minimum = "1", maximum = "100")
        Integer pageSize) {
}
