package org.open4goods.nudgerfrontapi.dto;

/**
 * Parameters controlling a product search query.
 */
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record SearchRequest(
        @Schema(description = "Search keywords", example = "eco detergent")
        String query,

        @Schema(description = "Minimum price filter", example = "0")
        Integer fromPrice,

        @Schema(description = "Maximum price filter", example = "50")
        Integer toPrice,

        @Schema(description = "Category identifiers", example = "[\"cleaning\"]")
        List<String> categories,

        @Schema(description = "Item condition", example = "new")
        String condition,

        @Schema(description = "Page number (1-based)", example = "1")
        Integer page,

        @Schema(description = "Page size", example = "20")
        Integer size,

        @Schema(description = "Sort by relevance", example = "true")
        boolean sort) {
}
