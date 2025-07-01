package org.open4goods.nudgerfrontapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pagination metadata.
 */
public record PageMetaDto(
        @Schema(description = "Zero-based page index")
        int number,
        @Schema(description = "Requested page size")
        int size,
        @Schema(description = "Total number of elements")
        long totalElements,
        @Schema(description = "Total number of pages")
        long totalPages) {
}
