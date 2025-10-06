package org.open4goods.nudgerfrontapi.dto.opendata;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Overview information about all available OpenData datasets.
 */
@Schema(description = "Aggregated information describing the full OpenData catalogue.")
public record OpenDataOverviewDto(
        @Schema(description = "Total number of products covered by all datasets, formatted using the requested locale.",
                example = "12\u00a0345")
        String totalProductCount,

        @Schema(description = "Number of datasets available for download, formatted using the requested locale.",
                example = "2")
        String datasetCount,

        @Schema(description = "Human readable size of all datasets combined, formatted using the requested locale.",
                example = "1,5\u00a0GB")
        String totalDatasetSize,

        @Schema(description = "Download restrictions applied when fetching the datasets.")
        OpenDataDownloadLimitsDto downloadLimits
) {
}
