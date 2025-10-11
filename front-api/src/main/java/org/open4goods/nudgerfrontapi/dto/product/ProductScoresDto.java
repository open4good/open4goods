package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Score information facet.
 */
public record ProductScoresDto(
        @Schema(description = "All scores keyed by their identifier")
        Map<String, ProductScoreDto> scores,
        @Schema(description = "Scores computed from real measurements")
        List<ProductScoreDto> realScores,
        @Schema(description = "Scores computed virtually")
        List<ProductScoreDto> virtualScores,
        @Schema(description = "Ecoscore when available", nullable = true)
        ProductScoreDto ecoscore,
        @Schema(description = "Score identifiers where the product ranks amongst the worst")
        Set<String> worstScores,
        @Schema(description = "Score identifiers where the product ranks amongst the best")
        Set<String> bestScores
) {
}
