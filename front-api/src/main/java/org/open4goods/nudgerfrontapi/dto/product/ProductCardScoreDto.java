package org.open4goods.nudgerfrontapi.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Score of a {@link ProductCardDto}: identifier and value, without the cardinalities and references of
 * {@link ProductScoreDto}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardScoreDto(
        @Schema(description = "Score identifier", example = "ECOSCORE")
        String id,
        @Schema(description = "Score value, on 20 for the ECOSCORE", example = "15.5", nullable = true)
        Double value
) {
}
