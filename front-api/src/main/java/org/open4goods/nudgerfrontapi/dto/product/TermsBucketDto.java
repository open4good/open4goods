package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single bucket returned by an Elastic terms aggregation.
 */
public record TermsBucketDto(
        @Schema(description = "Bucket key", example = "electronics")
        String key,
        @Schema(description = "Number of matching documents", example = "42")
        long count) {
}
