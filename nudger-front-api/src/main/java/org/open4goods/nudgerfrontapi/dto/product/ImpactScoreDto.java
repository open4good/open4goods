package org.open4goods.nudgerfrontapi.dto.product;

/**
 * Aggregated impact score information for a product.
 */
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

public record ImpactScoreDto(
        @Schema(description = "Individual scores by key", example = "{\"water\":1.0}")
        Map<String, Double> scores,

        @Schema(description = "Average score", example = "0.75")
        double average) {}
