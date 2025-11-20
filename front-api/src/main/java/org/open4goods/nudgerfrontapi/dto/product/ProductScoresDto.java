package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Score information facet.
 */
public record ProductScoresDto(
        @Schema(description = "All scores keyed by their identifier")
        Map<String, ProductScoreDto> scores,
        @Schema(description = "Ecoscore when available", nullable = true)
        ProductScoreDto ecoscore,
        @Schema(description = "Ecoscore derived rankings", nullable = true)
        ProductRankingDto ranking
) {
}
