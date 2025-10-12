package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing Icecat feature group metadata.
 */
public record ProductFeatureGroupDto(
        @Schema(description = "Identifier of the Icecat category feature group", example = "1234")
        Integer icecatCategoryFeatureGroupId
) {
}
