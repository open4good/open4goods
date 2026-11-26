package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Scores facet of a {@link ProductCardDto}: the ECOSCORE value and the ImpactScore status with its methodology
 * version.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardScoresDto(
        @Schema(description = "ECOSCORE keyed by its identifier; absent when not exposed for the product")
        Map<String, ProductCardScoreDto> scores,
        @Schema(description = "ImpactScore status under the vertical's current methodology", nullable = true)
        ProductImpactScoreStatusDto impactScore
) {
}
