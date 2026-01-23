package org.open4goods.nudgerfrontapi.dto.category;

import org.open4goods.model.vertical.scoring.ScoreDegeneratePolicy;
import org.open4goods.model.vertical.scoring.ScoreMissingValuePolicy;
import org.open4goods.model.vertical.scoring.ScoreTransform;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Scoring configuration exposed to the frontend.
 */
public record ScoreScoringConfigDto(
        @Schema(description = "Scale configuration for this score.")
        ScoreScaleDto scale,
        @Schema(description = "Normalization configuration for this score.")
        ScoreNormalizationConfigDto normalization,
        @Schema(description = "Value transform applied before normalization.")
        ScoreTransform transform,
        @Schema(description = "Policy used when a value is missing.")
        ScoreMissingValuePolicy missingValuePolicy,
        @Schema(description = "Policy used when distribution is degenerate.")
        ScoreDegeneratePolicy degenerateDistributionPolicy
) {
}
