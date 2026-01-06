package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for global product search.
 *
 * @param query      free-text query supplied by the caller
 * @param searchType search type controlling the starting mode (defaults to {@code auto})
 */
public record GlobalSearchRequestDto(
        @Schema(description = "Free-text query used to search across products.",
                example = "Fairphone 4")
        String query,
        @Schema(description = "Search type controlling the starting mode. Defaults to auto when omitted.",
                implementation = SearchType.class)
        SearchType searchType) {
}
