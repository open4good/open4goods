package org.open4goods.nudgerfrontapi.dto.search;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the minimum set of aggregations available for global search when no vertical is specified.
 */
public enum AllowedGlobalAggregations {
    @Schema(description = "Aggregate the number of offers available", example = "offersCount")
    offersCount("offersCount", AggregationRequestDto.AggType.range),
    @Schema(description = "Aggregate the minimum aggregated price", example = "price.minPrice.price")
    minPrice("price.minPrice.price", AggregationRequestDto.AggType.range),
    @Schema(description = "Aggregate the product condition (NEW, USED)", example = "price.conditions")
    productCondition("price.conditions", AggregationRequestDto.AggType.terms),
    @Schema(description = "Aggregate the impact score", example = "scores.ECOSCORE.value")
    ecoscore("scores.ECOSCORE.value", AggregationRequestDto.AggType.range);

    private final String fieldPath;
    private final AggregationRequestDto.AggType aggregationType;

    AllowedGlobalAggregations(String fieldPath, AggregationRequestDto.AggType aggregationType) {
        this.fieldPath = fieldPath;
        this.aggregationType = aggregationType;
    }

    /**
     * Return the Elasticsearch field path for the aggregation.
     *
     * @return field path used in the query
     */
    public String fieldPath() {
        return fieldPath;
    }

    /**
     * Return the aggregation type used to build the request.
     *
     * @return aggregation type definition
     */
    public AggregationRequestDto.AggType aggregationType() {
        return aggregationType;
    }

    private static final Map<String, AllowedGlobalAggregations> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(AllowedGlobalAggregations::fieldPath, f -> f));

    /**
     * Resolve an aggregation definition by its field path.
     *
     * @param fieldPath field path to resolve
     * @return optional matching enum value
     */
    public static Optional<AllowedGlobalAggregations> fromFieldPath(String fieldPath) {
        return Optional.ofNullable(LOOKUP.get(fieldPath));
    }
}
