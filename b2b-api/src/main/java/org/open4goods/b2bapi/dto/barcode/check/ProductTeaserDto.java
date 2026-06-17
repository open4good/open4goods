package org.open4goods.b2bapi.dto.barcode.check;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Currency;

/**
 * Minimal product teaser extracted from the nudger.fr index.
 * Exposes only enough data to identify the product without revealing the full offer list
 * (which is protected by the paid {@code product.price} facet).
 *
 * @param gtin normalized GTIN of the product
 * @param title product display name
 * @param coverImageUrl URL of the product cover image
 * @param offersCount total number of offers in the index
 * @param bestPrice lowest price available across all conditions
 * @param currency currency of the best price
 * @param productUrl link to the product page on nudger.fr
 */
@Schema(description = "Minimal product teaser from the nudger.fr product index.")
public record ProductTeaserDto(

        @Schema(description = "Normalized GTIN of the matched product.", example = "3017620422003")
        String gtin,

        @Schema(description = "Product display name.", example = "Nutella 400g", nullable = true)
        String title,

        @Schema(description = "Cover image URL.", example = "https://www.nudger.fr/images/products/3017620422003.webp", nullable = true)
        String coverImageUrl,

        @Schema(description = "Total number of offers in the index.", example = "12")
        int offersCount,

        @Schema(description = "Lowest price available across all conditions.", example = "4.99", nullable = true)
        Double bestPrice,

        @Schema(description = "Currency of the best price.", example = "EUR", nullable = true)
        Currency currency,

        @Schema(description = "Link to the product page on nudger.fr.", example = "https://www.nudger.fr/fr/product/3017620422003", nullable = true)
        String productUrl) {
}
