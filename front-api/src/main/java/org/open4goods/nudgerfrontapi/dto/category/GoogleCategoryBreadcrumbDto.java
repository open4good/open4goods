package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Breadcrumb element describing a taxonomy node along the path to a category.
 */
public record GoogleCategoryBreadcrumbDto(
        @Schema(description = "Google taxonomy identifier for the breadcrumb node.", example = "1234")
        Integer taxonomyId,
        @Schema(description = "Display name of the breadcrumb node localised with the requested language.", example = "Téléviseurs")
        String name,
        @Schema(description = "Slugified segment representing the node in URLs.", example = "televiseurs")
        String slug,
        @Schema(description = "Full slug path from the taxonomy root to this node.", example = "electronique/televiseurs")
        String path
) {
}
