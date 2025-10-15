package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing the Google taxonomy navigation nodes required by the frontend
 * to render the category tree.
 */
public record GoogleCategoryDto(
        @Schema(description = "Google taxonomy identifier of the category.", example = "499972")
        Integer googleCategoryId,

        @Schema(description = "Localised display label for the category.", example = "Téléviseurs")
        String title,

        @Schema(description = "Slug corresponding to the current category segment.", example = "televiseurs")
        String slug,

        @Schema(description = "Hierarchical path made of slug segments separated by '/' starting from the root.",
                example = "electronique/televiseurs")
        String path,

        @Schema(description = "Indicates whether the node has no children in the Google taxonomy tree.", example = "false")
        boolean leaf,

        @Schema(description = "True when a vertical configuration is directly associated with the node.", example = "true")
        boolean hasVertical,

        @Schema(description = "True when the node or one of its descendants exposes a vertical configuration.",
                example = "true")
        boolean hasVerticals,

        @Schema(description = "Summary description of the associated vertical when present.")
        VerticalConfigDto vertical,

        @Schema(description = "Direct children of the category limited to the requested depth.")
        List<GoogleCategoryDto> children
) {
}

