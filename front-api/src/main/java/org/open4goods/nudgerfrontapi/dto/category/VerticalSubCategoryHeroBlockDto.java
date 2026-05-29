package org.open4goods.nudgerfrontapi.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing localized markdown content displayed beside a sub-category hero.
 *
 * @param title localized title for the hero information card
 * @param body localized markdown body for the hero information card
 */
public record VerticalSubCategoryHeroBlockDto(
        @Schema(description = "Localized title displayed in the hero information card.", example = "Le saviez-vous :")
        String title,
        @Schema(description = "Localized markdown body displayed in the hero information card.")
        String body
) {
}
