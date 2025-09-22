package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing aggregated statistics about vertical categories.
 */
public record CategoriesStatsDto(
        @Schema(description = "Total number of enabled vertical configurations", example = "42")
        int enabledVerticalConfigs
) { }
