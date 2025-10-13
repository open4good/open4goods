package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single bucket produced by a search aggregation.
 */
public record AggregationBucketDto(
        @Schema(description = "Bucket identifier. For numeric histograms this is the lower bound.")
        String key,
        @Schema(description = "Upper bound when the bucket represents a numeric range.")
        Double to,
        @Schema(description = "Number of matching products contained in the bucket.")
        long count,
        @Schema(description = "Whether this bucket corresponds to documents missing the aggregated field.")
        boolean missing) {
}
