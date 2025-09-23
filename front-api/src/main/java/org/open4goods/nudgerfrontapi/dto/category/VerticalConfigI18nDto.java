package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Localised summary texts for a vertical category card.
 */
public record VerticalConfigI18nDto(
        @Schema(description = "Display title for the vertical in the target language.", example = "Téléviseurs")
        String title,
        @Schema(description = "Short description shown on the categories listing page.",
                example = "Découvrez les téléviseurs classés par impact environnemental.")
        String description,
        @Schema(description = "Slug used to build the URL of the vertical home page.", example = "televiseurs")
        String url
) {
}
