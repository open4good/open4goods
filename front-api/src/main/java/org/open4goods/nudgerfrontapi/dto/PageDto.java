package org.open4goods.nudgerfrontapi.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic page wrapper.
 */
public record PageDto<T>(
        @Schema(description = "Pagination metadata")
        PageMetaDto page,
        @Schema(description = "Current page content, to be localised using the domainLanguage query parameter when supported.")
        List<T> data) {
}
