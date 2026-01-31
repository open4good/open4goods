package org.open4goods.nudgerfrontapi.dto.search;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the minimum set of filters available for global search when no vertical is specified.
 */
public enum AllowedGlobalFilters {
    @Schema(description = "Filter on the number of offers available", example = "offersCount")
    offersCount("offersCount", FilterRequestDto.FilterValueType.numeric),
    @Schema(description = "Filter on the minimum aggregated price", example = "price.minPrice.price")
    minPrice("price.minPrice.price", FilterRequestDto.FilterValueType.numeric),
    @Schema(description = "Filter on the product condition (NEW, USED)", example = "price.conditions")
    productCondition("price.conditions", FilterRequestDto.FilterValueType.keyword),
    @Schema(description = "Filter on the manufacturing country", example = "gtinInfos.country")
    country("gtinInfos.country", FilterRequestDto.FilterValueType.keyword),
    @Schema(description = "Filter on the product ecoscore", example = "scores.ECOSCORE.value")
    ecoscore("scores.ECOSCORE.value", FilterRequestDto.FilterValueType.numeric);

    private final String fieldPath;
    private final FilterRequestDto.FilterValueType valueType;

    AllowedGlobalFilters(String fieldPath, FilterRequestDto.FilterValueType valueType) {
        this.fieldPath = fieldPath;
        this.valueType = valueType;
    }

    /**
     * Return the Elasticsearch field path for the filter.
     *
     * @return field path used in the query
     */
    public String fieldPath() {
        return fieldPath;
    }

    /**
     * Return the value type expected by the filter.
     *
     * @return filter value type
     */
    public FilterRequestDto.FilterValueType valueType() {
        return valueType;
    }

    private static final Map<String, AllowedGlobalFilters> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(AllowedGlobalFilters::fieldPath, f -> f));

    /**
     * Resolve a filter definition by its field path.
     *
     * @param fieldPath field path to resolve
     * @return optional matching enum value
     */
    public static Optional<AllowedGlobalFilters> fromFieldPath(String fieldPath) {
        return Optional.ofNullable(LOOKUP.get(fieldPath));
    }
}
