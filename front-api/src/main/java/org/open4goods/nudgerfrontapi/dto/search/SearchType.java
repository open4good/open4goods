package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumerates the search type requested by the client.
 */
@Schema(description = "Requested search type controlling the starting mode.")
public enum SearchType {
    @Schema(description = "Automatic mode that starts with exact vertical search and applies fallbacks.")
    auto,
    @Schema(description = "Start with exact vertical search.")
    exact_vertical,
    @Schema(description = "Start with global search across products without a vertical.")
    global,
    @Schema(description = "Start directly with semantic search.")
    semantic
}
