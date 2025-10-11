package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datasource level information related to the product.
 */
public record ProductDatasourcesDto(
        @Schema(description = "Codes assigned to the product by each datasource")
        Map<String, Long> datasourceCodes
) {
}
