package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datasource level information related to the product.
 *
 * @param datasourceCodes codes assigned to the product by each datasource, without the technical completion keys
 * @param descriptions    product descriptions by datasource
 * @param favicons        favicons of the datasources holding a description
 * @param sources         the sources of the product (the keys of {@code datasourceCodes}) with their portal URL and
 *                        the attribution they state (GOU-2302)
 */
public record ProductDatasourcesDto(
        @Schema(description = "Codes assigned to the product by each datasource")
        Map<String, Long> datasourceCodes,
        @Schema(description = "Product descriptions by datasource")
        Map<String, String> descriptions,
        @Schema(description = "Datasource favicons")
        Map<String, String> favicons,
        @Schema(description = "Sources contributing to the product (the keys of datasourceCodes), sorted by name, with "
                + "their portal URL and the licence and attribution they state. The attribute sourcing references "
                + "them by datasourceName.")
        List<ProductSourceDto> sources
) {
}
