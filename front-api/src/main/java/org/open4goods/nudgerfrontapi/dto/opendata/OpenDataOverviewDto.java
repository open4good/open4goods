package org.open4goods.nudgerfrontapi.dto.opendata;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Overview of the GTIN and ISBN datasets, read from the published manifest (GOU-2290).
 * <p>
 * The values cover the GTIN and ISBN entries of {@code manifest.json} only: the impact datasets describe the same
 * products and are not counted. Every field is null while the manifest is not configured
 * ({@code open-data-config.public-base-url} empty) or was never read successfully.
 * </p>
 */
@Schema(description = "Aggregated information describing the GTIN and ISBN datasets, read from the published manifest. "
        + "Fields are null while the manifest is not configured or not readable.")
public record OpenDataOverviewDto(
        @Schema(description = "Rows of the GTIN and ISBN files (sum of the manifest rows), formatted using the requested "
                + "locale; null when unknown.", example = "124 490 051", nullable = true)
        String totalProductCount,

        @Schema(description = "Number of GTIN and ISBN datasets listed in the manifest, formatted using the requested "
                + "locale; null when unknown.", example = "2", nullable = true)
        String datasetCount,

        @Schema(description = "Size of the GTIN and ISBN zips combined (sum of the manifest bytes), formatted using the "
                + "requested locale; null when unknown.", example = "6,6 GB", nullable = true)
        String totalDatasetSize
) {
}
