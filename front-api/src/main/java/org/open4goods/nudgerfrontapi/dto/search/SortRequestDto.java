package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes sort instructions accepted by the products endpoint.
 */
public record SortRequestDto(
        @Schema(description = "Sort options applied in order of appearance")
        List<SortOption> sorts) {

    /**
     * Single sort option definition.
     */
    public record SortOption(
            @Schema(description = "Field mapping used for sorting", example = "price.minPrice.price")
            String field,
            @Schema(description = "Sort order", defaultValue = "asc")
            SortOrder order) {
    }

    /**
     * Allowed sort orders.
     */
    public enum SortOrder {
        @Schema(description = "Ascending order")
        asc,
        @Schema(description = "Descending order")
        desc
    }
}
