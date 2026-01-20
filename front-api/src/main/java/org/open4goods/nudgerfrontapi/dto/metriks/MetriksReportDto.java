package org.open4goods.nudgerfrontapi.dto.metriks;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregated metriks report payload for the frontend.
 *
 * @param columns ordered list of date keys included in the report
 * @param rows list of metric rows
 */
public record MetriksReportDto(
        @Schema(description = "Ordered list of date keys", example = "[\"20250101\", \"20250108\"]")
        List<String> columns,
        @Schema(description = "Metric rows grouped by provider and event id")
        List<MetriksReportRowDto> rows
) {
}
