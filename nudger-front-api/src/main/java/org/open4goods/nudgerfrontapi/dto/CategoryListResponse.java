package org.open4goods.nudgerfrontapi.dto;

/**
 * Paginated list of product categories returned by the API.
 */
import java.util.List;

import org.open4goods.nudgerfrontapi.service.CategoryService.CategoryDto;
import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryListResponse(
        @Schema(description = "Current page index (1-based)", example = "1")
        int page,

        @Schema(description = "Page size", example = "10")
        int size,

        @Schema(description = "Total number of categories", example = "20")
        long total,

        @Schema(description = "Returned categories")
        List<CategoryDto> items) {
}
