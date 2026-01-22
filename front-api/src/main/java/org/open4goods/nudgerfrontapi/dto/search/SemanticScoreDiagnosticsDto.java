package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Diagnostics about semantic score distributions for debugging relevance.
 */
public record SemanticScoreDiagnosticsDto(
        @Schema(description = "Number of semantic hits considered for diagnostics", example = "12")
        int resultCount,
        @Schema(description = "Highest semantic score observed", example = "1.42")
        double topScore,
        @Schema(description = "Lowest semantic score observed", example = "0.92")
        double minScore,
        @Schema(description = "Average semantic score observed", example = "1.03")
        double avgScore,
        @Schema(description = "Standard deviation of semantic scores", example = "0.12")
        double scoreStdDev) {
}
