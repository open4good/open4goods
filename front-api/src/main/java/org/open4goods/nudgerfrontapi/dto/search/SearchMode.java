package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumerates the effective search modes used to produce a global search result.
 */
@Schema(description = "Effective search mode used to generate results.")
public enum SearchMode {
    @Schema(description = "Exact search within verticalised products.")
    exact_vertical,
    @Schema(description = "Global search across products without a vertical.")
    global,
    @Schema(description = "Semantic (vector-based) search fallback.")
    semantic
}
