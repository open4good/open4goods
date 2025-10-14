package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload grouping product field options by their scope.
 */
public record ProductFieldOptionsResponse(
        @Schema(description = "Fields that are always available regardless of the vertical.", implementation = FieldMetadataDto.class)
        List<FieldMetadataDto> global,
        @Schema(description = "Fields relating to ecological impact, including scores and eco filters.", implementation = FieldMetadataDto.class)
        List<FieldMetadataDto> impact,
        @Schema(description = "Fields exposing technical attributes available for the vertical.", implementation = FieldMetadataDto.class)
        List<FieldMetadataDto> technical
) {
}
