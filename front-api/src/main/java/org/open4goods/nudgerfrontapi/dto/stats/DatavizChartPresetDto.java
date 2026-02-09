package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing one chart card preset in the dataviz gallery.
 *
 * @param id stable chart identifier
 * @param chartType visualisation type expected by the frontend renderer
 * @param title localised chart title
 * @param description localised chart description
 * @param queryPreset backend preset key used to execute aggregations
 * @param hasRole optional role required to display the chart
 * @param exportCsv whether CSV export is enabled for this chart
 */
@Schema(description = "Dataviz chart preset metadata.")
public record DatavizChartPresetDto(
        @Schema(description = "Chart identifier.", example = "products-by-brand")
        String id,
        @Schema(description = "Chart type.", example = "bar")
        String chartType,
        @Schema(description = "Localised chart title.")
        String title,
        @Schema(description = "Localised chart description.")
        String description,
        @Schema(description = "Backend query preset key.", example = "productsByBrand")
        String queryPreset,
        @Schema(description = "Role required to render the chart in the frontend.", nullable = true, example = "ROLE_FRONTEND")
        String hasRole,
        @Schema(description = "Whether CSV export is available for this chart.")
        boolean exportCsv) {
}
