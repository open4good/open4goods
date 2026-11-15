package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A position label of the ImpactScore: products ranked better than at least {@code minBetterThan} of the rated
 * products get this label.
 */
public record ImpactScorePositionLabelDto(
        @Schema(description = "Label key, resolved by the client (i18n)", example = "excellent")
        String level,
        @Schema(description = "Lowest share of rated products, between 0 and 1, the product must rank better than", example = "0.9",
                minimum = "0", maximum = "1")
        Double minBetterThan
) {
}
