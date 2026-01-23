package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the normalization scale bounds exposed to clients.
 */
public record ScoreScaleDto(
        @Schema(description = "Minimum value for the normalized score scale.", example = "0.0")
        Double min,
        @Schema(description = "Maximum value for the normalized score scale.", example = "5.0")
        Double max
) {
}
