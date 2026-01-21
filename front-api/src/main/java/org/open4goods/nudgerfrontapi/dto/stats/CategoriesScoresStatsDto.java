package org.open4goods.nudgerfrontapi.dto.stats;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing per-category score cardinalities.
 */
public record CategoriesScoresStatsDto(
        @Schema(description = "Per-category score cardinalities, keyed by vertical id and score name.")
        Map<String, Map<String, CategoryScoreCardinalitiesDto>> scoresByCategory
) {
}
