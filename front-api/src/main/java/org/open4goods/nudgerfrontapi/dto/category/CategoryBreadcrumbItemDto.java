package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a single breadcrumb item associated with a Google taxonomy
 * category.
 */
public record CategoryBreadcrumbItemDto(
        @Schema(description = "Localised label of the taxonomy node displayed in the breadcrumb.",
                example = "Électroménager")
        String title,

        @Schema(description = "Localised URL path for the taxonomy node, resolved against the domain language.",
                example = "/categories/electromenager")
        String link
) {
}

