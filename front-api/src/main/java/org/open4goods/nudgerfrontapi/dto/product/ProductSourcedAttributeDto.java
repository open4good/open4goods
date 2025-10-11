package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Attribute value contributed by a specific datasource.
 */
public record ProductSourcedAttributeDto(
        @Schema(description = "Datasource providing the attribute", example = "example-shop")
        String datasourceName,
        @Schema(description = "Attribute value", example = "55")
        String value,
        @Schema(description = "Attribute language when provided", example = "fr", nullable = true)
        String language,
        @Schema(description = "Icecat taxonomy identifier for the attribute", nullable = true)
        Integer icecatTaxonomyId,
        @Schema(description = "Attribute label provided by the datasource", nullable = true)
        String name
) {
}
