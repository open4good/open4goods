package org.open4goods.nudgerfrontapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Association information returned by API.
 */
public record AssociationDto(
        @Schema(description = "Association identifier", example = "asso-123")
        String id,

        @Schema(description = "Association name", example = "Zero Waste France")
        String name) {
}
