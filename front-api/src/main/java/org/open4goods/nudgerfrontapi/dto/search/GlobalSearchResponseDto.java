package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload returned by the global search endpoint.
 */
public record GlobalSearchResponseDto(
        @Schema(description = "Vertical groups produced by the primary search pass")
        List<GlobalSearchVerticalGroupDto> verticalGroups,
        @Schema(description = "Semantic results that have no vertical assigned")
        List<GlobalSearchResultDto> missingVerticalResults,
        @Schema(description = "Category suggestion if a strict match is found")
        SearchSuggestCategoryDto verticalCta,
        @Schema(description = "Semantic score diagnostics when enabled", nullable = true)
        SemanticScoreDiagnosticsDto semanticDiagnostics) {
}
