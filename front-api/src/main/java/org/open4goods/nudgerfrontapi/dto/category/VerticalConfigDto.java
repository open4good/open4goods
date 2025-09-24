package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Summary representation of a vertical configuration exposed to the frontend.
 */
public record VerticalConfigDto(
        @Schema(description = "Unique identifier of the vertical configuration.", example = "tv")
        String id,
        @Schema(description = "Indicates whether the vertical is exposed to end-users.", example = "true")
        boolean enabled,
        @Schema(description = "Google taxonomy identifier associated with this vertical.", example = "404")
        Integer googleTaxonomyId,
        @Schema(description = "Icecat taxonomy identifier associated with this vertical.", example = "1584")
        Integer icecatTaxonomyId,
        @Schema(description = "Display order used to render the category list.", example = "1")
        Integer order,
        @Schema(description = "Thumbnail image representing the vertical. TODO(front-api): populate when media assets are defined.",
                nullable = true)
        String imageThumbnail,
        @Schema(description = "Primary image for the vertical hero section. TODO(front-api): populate when media assets are defined.",
                nullable = true)
        String image,
        @Schema(description = "Localised singular label for the vertical. TODO(front-api): populate when naming strategy is available.",
                nullable = true)
        String singularName,
        @Schema(description = "Localised plural label for the vertical. TODO(front-api): populate when naming strategy is available.",
                nullable = true)
        String pluralName,
        @Schema(description = "Localised home title for the vertical.", example = "Téléviseurs")
        String verticalHomeTitle,
        @Schema(description = "Localised home description for the vertical.", example = "Comparez les téléviseurs responsables")
        String verticalHomeDescription,
        @Schema(description = "Localised home URL slug for the vertical.", example = "televiseurs")
        String verticalHomeUrl
) {
}
