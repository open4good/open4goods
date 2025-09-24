package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing a single impact score criterion with localised metadata.
 */
public record ImpactScoreCriteriaDto(
        @Schema(description = "Key identifying the criterion.", example = "repairability")
        String key,
        @Schema(description = "Localised title describing the criterion.", example = "Réparabilité")
        String title,
        @Schema(description = "Localised description providing more context.", example = "Capacité à être réparé facilement")
        String description
) {
}
