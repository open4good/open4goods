package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing expandable markdown copy for a vertical sub-category page.
 *
 * @param title localized title for the read-more section
 * @param shortText localized short markdown text visible by default
 * @param longText localized long markdown text revealed after expansion
 */
public record VerticalSubCategoryReadMoreDto(
        @Schema(description = "Localized title for the read-more section.", example = "Comment choisir un lave-vaisselle sous lavabo ?")
        String title,
        @Schema(description = "Localized short markdown text visible before expansion.")
        String shortText,
        @Schema(description = "Localized long markdown text revealed after expansion.")
        String longText
) {
}
