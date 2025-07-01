package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Wrapper around the paginated product list with optional aggregations.
 */
public record ProductPageAggsDto(
        @Schema(description = "Page of products")
        Page<ProductDto> page,
        @Schema(description = "Aggregations keyed by field name")
        Map<String, @ArraySchema(schema = @Schema(implementation = TermsBucketDto.class)) List<TermsBucketDto>> aggs) {
}
