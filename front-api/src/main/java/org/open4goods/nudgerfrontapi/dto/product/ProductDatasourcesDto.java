package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datasource level information related to the product.
 */
public record ProductDatasourcesDto(
        @Schema(description = "Codes assigned to the product by each datasource")
        Map<String, Long> datasourceCodes,
        @Schema(description = "Categories assigned by the contributing datasources")
        Map<String, String> categoriesByDatasource,
        @Schema(description = "Union of datasource categories")
        Set<String> datasourceCategories,
        @Schema(description = "Datasource categories excluding the shortest one")
        Set<String> datasourceCategoriesWithoutShortest
) {
}
