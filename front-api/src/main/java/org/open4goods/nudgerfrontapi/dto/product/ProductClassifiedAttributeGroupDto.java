package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a group of Icecat classified attributes.
 */
public record ProductClassifiedAttributeGroupDto(
        @Schema(description = "Localized display name of the feature group", example = "General")
        String name,
        @Schema(description = "All attributes associated with the feature group")
        List<ProductAttributeDto> attributes,
        @Schema(description = "Attributes flagged as features in the Icecat taxonomy")
        List<ProductAttributeDto> features,
        @Schema(description = "Attributes flagged as unfeatures in the Icecat taxonomy")
        List<ProductAttributeDto> unFeatures
) {
}
