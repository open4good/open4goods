package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pagination metadata for global search results.
 *
 * <p>This DTO provides pagination information for the missing-vertical results
 * returned by the global search endpoint, allowing clients to navigate through
 * large result sets.</p>
 *
 * @param number        zero-based page index
 * @param size          requested page size
 * @param totalElements total number of elements across all pages
 * @param totalPages    total number of pages
 */
public record GlobalSearchPageMetaDto(
        @Schema(description = "Zero-based page index", example = "0")
        int number,
        @Schema(description = "Requested page size", example = "20")
        int size,
        @Schema(description = "Total number of elements", example = "1250")
        long totalElements,
        @Schema(description = "Total number of pages", example = "63")
        int totalPages) {
}
