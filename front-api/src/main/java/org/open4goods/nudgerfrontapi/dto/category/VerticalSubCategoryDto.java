package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;

import org.open4goods.model.vertical.SubsetCriteria;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing a localized search-intent page configured under a vertical.
 *
 * @param id stable identifier of the sub-category
 * @param slug localized URL segment below the parent category
 * @param h1Title localized page title used as the category hero title
 * @param description localized page description used for hero and SEO copy
 * @param heroBlock optional localized markdown information block displayed in the hero
 * @param image optional image overriding the parent category image
 * @param activatedFilters criteria always applied to the product listing
 */
public record VerticalSubCategoryDto(
        @Schema(description = "Stable identifier of the sub-category.", example = "dishwasher-under-sink")
        String id,
        @Schema(description = "Localised URL segment below the parent category.", example = "lave-vaisselle-sous-lavabo")
        String slug,
        @Schema(description = "Localised H1 title displayed on the sub-category page.", example = "Lave-vaisselle sous lavabo")
        String h1Title,
        @Schema(description = "Localised description displayed on the sub-category page.")
        String description,
        @Schema(description = "Optional localized markdown information block displayed in the hero.")
        VerticalSubCategoryHeroBlockDto heroBlock,
        @Schema(description = "Optional image overriding the parent category image.")
        String image,
        @Schema(description = "Criteria always applied to the product listing on this sub-category page.")
        List<SubsetCriteria> activatedFilters
) {
}
