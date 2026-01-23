package org.open4goods.nudgerfrontapi.dto.category;

import org.open4goods.model.vertical.scoring.ScoreNormalizationMethod;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Normalization configuration exposed to the frontend.
 */
public record ScoreNormalizationConfigDto(
        @Schema(description = "Normalization method used for this score.")
        ScoreNormalizationMethod method,
        @Schema(description = "Parameters used by the normalization method.")
        ScoreNormalizationParamsDto params
) {
}
