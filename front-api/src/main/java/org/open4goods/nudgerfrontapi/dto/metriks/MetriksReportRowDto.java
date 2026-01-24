package org.open4goods.nudgerfrontapi.dto.metriks;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Row containing metriks data for one provider/event pair.
 *
 * @param provider event provider
 * @param eventId event identifier
 * @param name display name
 * @param description display description
 * @param unit data unit
 * @param eventUrl optional external link
 * @param values list of values ordered by date key
 */
public record MetriksReportRowDto(
        @Schema(description = "Provider identifier", example = "github")
        String provider,
        @Schema(description = "Event identifier", example = "github_prs_merged_count")
        String eventId,
        @Schema(description = "Display name", example = "PR mergées")
        String name,
        @Schema(description = "Display description", example = "Nombre de pull requests mergées sur la période")
        String description,
        @Schema(description = "Unit", example = "count", allowableValues = {"count", "bytes", "percent", "currency"})
        String unit,
        @Schema(description = "Optional external URL", nullable = true, example = "https://github.com/open4good/open4goods")
        String eventUrl,
        @Schema(description = "Metric values aligned with columns")
        List<MetriksReportValueDto> values
) {
}
