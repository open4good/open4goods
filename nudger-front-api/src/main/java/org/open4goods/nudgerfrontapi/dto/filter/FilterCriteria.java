package org.open4goods.nudgerfrontapi.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Criteria parsed from a filter query parameter, e.g. {@code filter[price_lt]=20}.
 *
 * @param field    the entity field to test
 * @param operator the comparison operator
 * @param value    the comparison value as String
 */
public record FilterCriteria(
        @Schema(description = "Entity field name", example = "price")
        String field,
        @Schema(description = "Comparison operator", example = "LT")
        FilterOperator operator,
        @Schema(description = "Filter value as string", example = "20")
        String value) {
}
