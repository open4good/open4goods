package org.open4goods.nudgerfrontapi.dto.category;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Parameters used by normalization strategies.
 */
public record ScoreNormalizationParamsDto(
        @Schema(description = "Sigma factor used by SIGMA normalization.", example = "2.0")
        Double sigmaK,
        @Schema(description = "Fixed minimum bound used by MINMAX_FIXED normalization.", example = "0.0")
        Double fixedMin,
        @Schema(description = "Fixed maximum bound used by MINMAX_FIXED normalization.", example = "10.0")
        Double fixedMax,
        @Schema(description = "Lower quantile used by MINMAX_QUANTILE normalization.", example = "0.05")
        Double quantileLow,
        @Schema(description = "Upper quantile used by MINMAX_QUANTILE normalization.", example = "0.95")
        Double quantileHigh,
        @Schema(description = "Mapping table used by FIXED_MAPPING normalization.")
        Map<String, Double> mapping,
        @Schema(description = "Constant value used by CONSTANT normalization.", example = "2.5")
        Double constantValue,
        @Schema(description = "Threshold used by BINARY normalization.", example = "1.0")
        Double threshold,
        @Schema(description = "When true, BINARY normalization passes when value >= threshold.", example = "true")
        Boolean greaterIsPass
) {
}
