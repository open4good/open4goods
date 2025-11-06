package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregated sourcing information for a product attribute.
 * <p>
 * This structure exposes the resolved best value alongside the list of
 * datasource contributions and a conflict indicator so that the frontend can
 * communicate data quality hints to end users.
 * </p>
 */
public record ProductAttributeSourceDto(
        @Schema(description = "Resolved best value for the attribute", example = "55")
        String bestValue,
        @Schema(description = "Datasource contributions used to compute the best value")
        Set<ProductSourcedAttributeDto> sources,
        @Schema(description = "Flag indicating whether conflicting datasource values exist")
        boolean conflicts
) {
}
