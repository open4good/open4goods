package org.open4goods.nudgerfrontapi.dto;

/**
 * Parameters for listing categories.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryRequest(
        @Schema(description = "Page number (1-based)", example = "1")
        Integer page,

        @Schema(description = "Requested page size", example = "10")
        Integer size,

        @Schema(description = "Whether to include children", example = "true")
        boolean include) {
}
