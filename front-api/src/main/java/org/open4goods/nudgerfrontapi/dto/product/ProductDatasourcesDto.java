package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datasource level information related to the product.
 */
public record ProductDatasourcesDto(
        @Schema(description = "Codes assigned to the product by each datasource")
        Map<String, Long> datasourceCodes,
        @Schema(description = "Product descriptions by datasource")
        Map<String, String> descriptions,
        @Schema(description = "Datasource favicons")
        Map<String, String> favicons
) {
}
