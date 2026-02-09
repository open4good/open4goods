package org.open4goods.nudgerfrontapi.dto.stats;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing the dataviz dashboard plan for one vertical.
 *
 * @param verticalId identifier of the vertical configuration
 * @param defaultFilters filters applied by default before user overrides
 * @param charts chart presets available for the dashboard gallery
 */
@Schema(description = "Dataviz dashboard plan resolved for a vertical.")
public record VerticalDatavizPlanDto(
        @Schema(description = "Vertical identifier.", example = "televisions")
        String verticalId,
        @Schema(description = "Default filters injected by the backend.")
        List<DatavizDefaultFilterDto> defaultFilters,
        @Schema(description = "Chart presets available for the current vertical.")
        List<DatavizChartPresetDto> charts) {
}
