package org.open4goods.nudgerfrontapi.dto;

/**
 * Metadata describing a request lifecycle.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record RequestMetadata(
        @Schema(description = "Request start timestamp ms", example = "1690972800000", nullable = true)
        Long startDate,

        @Schema(description = "Request end timestamp ms", example = "1690972810000", nullable = true)
        Long endDate,

        @Schema(description = "Internal fishtag", example = "front-home", nullable = true)
        String fishtag) {
}
