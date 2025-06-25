package org.open4goods.nudgerfrontapi.dto;

/**
 * Simple statistics information returned by the API.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record StatsDto(
        @Schema(description = "Number of indexed products", example = "152")
        long products,

        @Schema(description = "Number of partner organisations", example = "6")
        long partners) {
}
