package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload grouping product field options by their scope.
 */
@Schema(name = "ProductFieldOptionsResponse", description = "Product fields available for filtering or sorting, split by scope.")
public record ProductFieldOptionsResponse(
        @ArraySchema(
                arraySchema = @Schema(description = "Fields that are always available regardless of the vertical."),
                schema = @Schema(implementation = FieldMetadataDto.class)
        )
        List<FieldMetadataDto> global,
        @ArraySchema(
                arraySchema = @Schema(description = "Fields relating to ecological impact scores only."),
                schema = @Schema(implementation = FieldMetadataDto.class)
        )
        List<FieldMetadataDto> impact,
        @ArraySchema(
                arraySchema = @Schema(description = "Fields exposing technical attributes available for the vertical."),
                schema = @Schema(implementation = FieldMetadataDto.class)
        )
        List<FieldMetadataDto> technical
) {
}
