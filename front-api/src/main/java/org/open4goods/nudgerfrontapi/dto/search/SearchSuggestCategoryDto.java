package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a category suggestion returned by the search suggest endpoint.
 */
public record SearchSuggestCategoryDto(
        @Schema(description = "Identifier of the matched vertical.", example = "tv")
        String verticalId,

        @Schema(description = "Absolute URL to the small vertical illustration used in suggestion cards.",
                example = "https://cdn.nudger.fr/images/verticals/tv-100.webp")
        String imageSmall,

        @Schema(description = "Localised title of the vertical home page.", example = "Téléviseurs")
        String verticalHomeTitle,

        @Schema(description = "Localised URL slug leading to the vertical home page.", example = "televiseurs")
        String verticalHomeUrl
) {
}
