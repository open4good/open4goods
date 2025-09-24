package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;

import org.open4goods.model.vertical.SubsetCriteria;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing vertical subset configuration with localised labels.
 */
public record VerticalSubsetDto(
        @Schema(description = "Identifier of the subset.", example = "premium")
        String id,
        @Schema(description = "Group identifier allowing subsets to be clustered together.")
        String group,
        @Schema(description = "Criteria defining the subset membership.")
        List<SubsetCriteria> criterias,
        @Schema(description = "Image illustrating the subset.")
        String image,
        @Schema(description = "Localised URL pointing to the subset page.")
        String url,
        @Schema(description = "Localised caption displayed when listing subsets.")
        String caption,
        @Schema(description = "Localised title displayed for the subset.")
        String title,
        @Schema(description = "Localised description detailing the subset purpose.")
        String description
) {
}
