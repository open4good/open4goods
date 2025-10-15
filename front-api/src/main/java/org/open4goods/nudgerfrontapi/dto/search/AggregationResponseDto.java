package org.open4goods.nudgerfrontapi.dto.search;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Container describing the aggregations computed for a search query.
 */
public record AggregationResponseDto(
        @Schema(description = "Aggregation name mirroring the request payload.")
        String name,
        @Schema(description = "Aggregated field")
        String field,
        @Schema(description = "Aggregation type", implementation = AggregationRequestDto.AggType.class)
        AggregationRequestDto.AggType type,
        @Schema(description = "Buckets yielded by the aggregation")
        List<AggregationBucketDto> buckets,
        @Schema(description = "Minimum observed value when applicable")
        Double min,
        @Schema(description = "Maximum observed value when applicable")
        Double max) {
}
