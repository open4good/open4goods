package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a default filter enforced by the dataviz API.
 *
 * @param field Elasticsearch field mapping targeted by the filter
 * @param operator operator used to evaluate the filter
 * @param min lower bound for numeric range filters
 * @param max upper bound for numeric range filters
 * @param minRelative lower bound expressed as a relative date expression
 */
@Schema(description = "Default filter injected by the dataviz backend.")
public record DatavizDefaultFilterDto(
        @Schema(description = "Field mapping targeted by the filter.", example = "lastChange")
        String field,
        @Schema(description = "Filter operator.", example = "range")
        String operator,
        @Schema(description = "Inclusive lower bound for numeric ranges.", nullable = true, example = "1")
        Double min,
        @Schema(description = "Inclusive upper bound for numeric ranges.", nullable = true)
        Double max,
        @Schema(description = "Relative date lower bound for temporal fields.", nullable = true, example = "now-2d")
        String minRelative) {
}
