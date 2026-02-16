package org.open4goods.nudgerfrontapi.dto.stats;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO wrapping the aggregation results of a chart query.
 *
 * <p>
 * Each response carries the chart metadata alongside ordered labels and values
 * ready for direct consumption by the frontend charting library (ECharts).
 * A raw bucket list is also included for renderers that need lower-level access.
 * </p>
 *
 * @param chartId     chart preset identifier that produced this result
 * @param chartType   visualisation type (bar, line, donut, etc.)
 * @param title       localised chart title
 * @param description localised chart description
 * @param labels      ordered category labels for the X axis or legend
 * @param values      ordered numeric values matching each label
 * @param totalHits   total number of matching documents in the query scope
 * @param metadata    optional additional metadata (e.g., unit, currency)
 */
@Schema(description = "Response wrapping chart aggregation results.")
public record DatavizChartQueryResponseDto(
        @Schema(description = "Chart preset identifier.", example = "products-by-brand")
        String chartId,

        @Schema(description = "Chart type hint for the frontend renderer.", example = "bar")
        String chartType,

        @Schema(description = "Localised chart title.")
        String title,

        @Schema(description = "Localised chart description.")
        String description,

        @Schema(description = "Ordered category labels for the chart.")
        List<String> labels,

        @Schema(description = "Ordered numeric values matching each label.")
        List<Double> values,

        @Schema(description = "Total number of matching documents in the query scope.")
        long totalHits,

        @Schema(description = "Optional key-value metadata such as unit or currency.", nullable = true)
        java.util.Map<String, String> metadata) {
}
