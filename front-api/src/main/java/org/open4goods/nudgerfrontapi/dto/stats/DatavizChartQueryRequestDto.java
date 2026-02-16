package org.open4goods.nudgerfrontapi.dto.stats;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for executing a chart aggregation query against a vertical.
 *
 * <p>
 * The frontend sends a chart identifier that maps to a pre-configured aggregation
 * definition on the backend. Optional filter overrides allow the UI to refine the
 * default scope without altering the original chart definition.
 * </p>
 *
 * @param chartId       stable identifier of the chart preset to execute
 * @param filterOverrides optional filter overrides applied on top of the default
 *                        filters defined by the dataviz plan
 */
@Schema(description = "Request to execute a chart aggregation query for a dataviz preset.")
public record DatavizChartQueryRequestDto(
        @NotBlank
        @Schema(description = "Chart preset identifier to execute.", example = "products-by-brand")
        String chartId,

        @Schema(description = "Optional filter overrides applied on top of plan defaults.", nullable = true)
        List<DatavizFilterOverrideDto> filterOverrides) {

    /**
     * Single filter override adjusting a default filter field.
     *
     * @param field       Elasticsearch field targeted by the filter
     * @param min         optional lower bound for numeric/date ranges
     * @param max         optional upper bound for numeric/date ranges
     * @param minRelative optional relative date expression for temporal lower bounds
     */
    @Schema(description = "Filter override adjusting a default filter field.")
    public record DatavizFilterOverrideDto(
            @NotBlank
            @Schema(description = "Field mapping targeted by the filter.", example = "lastChange")
            String field,

            @Schema(description = "Inclusive lower bound for numeric ranges.", nullable = true)
            Double min,

            @Schema(description = "Inclusive upper bound for numeric ranges.", nullable = true)
            Double max,

            @Schema(description = "Relative date lower bound for temporal fields.", nullable = true, example = "now-7d")
            String minRelative) {
    }
}
