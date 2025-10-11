package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Captures the brand/model identity of the product together with the different
 * alternative identifiers detected across datasources.
 */
public record ProductIdentityDto(
        @Schema(description = "Preferred brand extracted from referential attributes", example = "Sony")
        String brand,
        @Schema(description = "Preferred model extracted from referential attributes", example = "WH-1000XM5")
        String model,
        @Schema(description = "Best human readable name synthesised from brand and model when available", example = "Sony WH-1000XM5")
        String bestName,
        @Schema(description = "Randomly selected model amongst the known alternatives", example = "WH-1000XM5B")
        String randomModel,
        @Schema(description = "Shortest known model value", example = "XM5")
        String shortestModel,
        @Schema(description = "Set of alternative model identifiers discovered across datasources")
        Set<String> akaModels,
        @Schema(description = "Alternative brand values keyed by datasource")
        Map<String, String> akaBrandsByDatasource,
        @Schema(description = "Distinct set of alternative brand values")
        Set<String> akaBrands
) {
}
