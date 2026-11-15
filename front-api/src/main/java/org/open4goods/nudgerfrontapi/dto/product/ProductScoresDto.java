package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;
import java.util.Set;

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
        ProductRankingDto ranking,
        @Schema(description = "ImpactScore status under the vertical's current methodology; null for a product without vertical. When the product's score belongs to another methodology version, ecoscore, scores.ECOSCORE and ranking are not exposed.",
                nullable = true)
        ProductImpactScoreStatusDto impactScore
) {
}
