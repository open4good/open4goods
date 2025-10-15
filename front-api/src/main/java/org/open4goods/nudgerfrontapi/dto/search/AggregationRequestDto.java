package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Request wrapper for search aggregations.
 */
public record AggregationRequestDto(
        @Schema(description = "List of aggregations to compute")
        List<Agg> aggs) {

    /** Aggregation definition. */
    public record Agg(
            @Schema(description = "Aggregation name", example = "aggName")
            String name,
            @Schema(description = "Field mapping to aggregate", example = "price.minPrice.price")
            String field,
            @Schema(description = "Aggregation type", implementation = AggType.class)
            AggType type,
            @Schema(description = "Minimum value for range aggregations")
            Double min,
            @Schema(description = "Maximum value for range aggregations")
            Double max,
            @Schema(description = "Maximum number of buckets to return. For range aggregations this represents the desired bucket count.", defaultValue = "10")
            Integer buckets,
            @Schema(description = "Interval size for range aggregations. When provided it overrides the automatic bucket interval computation.")
            Double step) {
    }

    /** Supported aggregation types. */
    public enum AggType { terms, range }
}
