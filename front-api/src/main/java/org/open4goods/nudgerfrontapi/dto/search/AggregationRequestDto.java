package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoAggregatableFields;

/**
 * Request wrapper for search aggregations.
 */
public record AggregationRequestDto(
        @Schema(description = "List of aggregations to compute")
        List<Agg> aggs) {

    /** Aggregation definition. */
    public record Agg(
            @Schema(description = "Aggregation name", example = "byBrand")
            String name,
            @Schema(description = "Field to aggregate",
                    implementation = ProductDtoAggregatableFields.class)
            ProductDtoAggregatableFields field,
            @Schema(description = "Aggregation type", implementation = AggType.class)
            AggType type,
            @Schema(description = "Sub aggregation")
            Agg subAgg,
            @Schema(description = "Minimum value for range aggregations")
            Double min,
            @Schema(description = "Maximum value for range aggregations")
            Double max) {
    }

    /** Supported aggregation types. */
    public enum AggType { terms, range }
}
