package org.open4goods.nudgerfrontapi.dto.category;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO wrapping the attributes configuration with localised labels resolved.
 */
public record AttributesConfigDto(
        @Schema(description = "Attribute configurations defined for the vertical.")
        List<AttributeConfigDto> configs,
        @Schema(description = "Attribute values considered as featured for highlighting.")
        Set<String> featuredValues,
        @Schema(description = "Attribute names excluded from aggregation.")
        Set<String> exclusions
) {
}
