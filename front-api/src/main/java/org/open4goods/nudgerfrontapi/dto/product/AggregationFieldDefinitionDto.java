package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.search.AggregationBucketDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes default aggregation settings and dataset wide statistics for a field.
 *
 * <p>
 * The structure is consumed by the frontend to seed charts and range selectors without issuing
 * an additional roundtrip to the search API.
 * </p>
 */
public record AggregationFieldDefinitionDto(
        @Schema(description = "Minimum value observed across the full product set.", nullable = true)
        Double min,
        @Schema(description = "Maximum value observed across the full product set.", nullable = true)
        Double max,
        @Schema(description = "Default interval to use for range aggregations when building histograms.", nullable = true)
        Double interval,
        @Schema(description = "Whether an additional bucket gathering documents with missing values should be computed.")
        boolean includeMissing,
        @Schema(description = "Known buckets for categorical aggregations together with their document counts.", nullable = true)
        List<AggregationBucketDto> buckets
) {
}
