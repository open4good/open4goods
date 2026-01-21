package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cardinality statistics for a score in absolute and relative modes.
 */
public record CategoryScoreCardinalitiesDto(
        @Schema(description = "Absolute cardinality statistics", nullable = true)
        ScoreCardinalityDto absolute,
        @Schema(description = "Relative cardinality statistics", nullable = true)
        ScoreCardinalityDto relativ
) {
}
