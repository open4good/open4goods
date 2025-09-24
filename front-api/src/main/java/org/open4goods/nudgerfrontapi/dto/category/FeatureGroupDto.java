package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing feature group ordering with localised names.
 */
public record FeatureGroupDto(
        @Schema(description = "Identifier of the Icecat feature group mapped to this entry.")
        Integer icecatCategoryFeatureGroupId,
        @Schema(description = "Localised display name of the feature group.")
        String name,
        @Schema(description = "Identifiers of the Icecat features that belong to this group.")
        List<Integer> featuresId
) {
}
