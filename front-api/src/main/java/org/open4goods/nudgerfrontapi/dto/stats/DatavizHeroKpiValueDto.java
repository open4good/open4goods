package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single KPI value for the dataviz hero section.
 *
 * @param id        unique identifier (e.g. "avg-price")
 * @param label     display label (e.g. "Average Price")
 * @param value     the value (numeric or string)
 * @param unit      unit suffix (e.g. "€", "kg")
 * @param helpText  optional tooltip/description
 */
@Schema(description = "Single KPI value for the dataviz hero section.")
public record DatavizHeroKpiValueDto(
        @Schema(description = "Unique identifier", example = "avg-price")
        String id,

        @Schema(description = "Display label", example = "Average Price")
        String label,

        @Schema(description = "The computed value", example = "459.90")
        Object value,

        @Schema(description = "Unit suffix", example = "€")
        String unit,

        @Schema(description = "Optional tooltip or description", example = "Computed from active offers")
        String helpText
) {
}
