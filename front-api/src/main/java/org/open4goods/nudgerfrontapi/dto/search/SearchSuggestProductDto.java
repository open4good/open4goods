package org.open4goods.nudgerfrontapi.dto.search;

import org.open4goods.model.price.Currency;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing the lightweight information for a product suggestion.
 */
public record SearchSuggestProductDto(
        @Schema(description = "Model extracted from referential attributes.", example = "QE55QN90A")
        String model,

        @Schema(description = "Brand extracted from referential attributes.", example = "Samsung")
        String brand,

        @Schema(description = "GTIN associated with the product when available.", example = "8806092074061")
        String gtin,

        @Schema(description = "Absolute path to the cover image of the product if any.",
                example = "https://cdn.nudger.fr/images/products/8806092074061.webp")
        String coverImagePath,

        @Schema(description = "Identifier of the vertical owning the product.", example = "tv")
        String verticalId,

        @Schema(description = "Eco-score value computed for the product when available.", example = "72.5")
        Double ecoscoreValue,

        @Schema(description = "Best price available for the product when indexed.", example = "429.99")
        Double bestPrice,

        @Schema(description = "Currency associated with the best price when available.", example = "EUR")
        Currency bestPriceCurrency,

        @Schema(description = "Native Elasticsearch score associated with the hit.", example = "1.85")
        Double score,
        
        @Schema(description = "Pretty name of the product.", example = "Samsung TV 55 inches")
        String prettyName
) {
}
