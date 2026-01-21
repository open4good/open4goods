package org.open4goods.nudgerfrontapi.dto.stats;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing per-category cardinalities for a single score.
 */
public record CategoriesScoreStatsDto(
        @Schema(description = "Score name used to compute the statistics", example = "ECOSCORE")
        String scoreName,
        @Schema(description = "Per-category cardinalities for the requested score, keyed by vertical id.")
        Map<String, CategoryScoreCardinalitiesDto> scoresByCategory
) {
}
