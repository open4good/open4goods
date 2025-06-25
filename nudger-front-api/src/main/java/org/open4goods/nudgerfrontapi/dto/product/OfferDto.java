package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

public record OfferDto(
        @Schema(description = "Nom de la source de données (ex: Amazon, Leclerc)", example = "Amazon")
        String datasourceName,

        @Schema(description = "Nom de l'offre ou du produit tel qu'affiché par le commerçant", example = "Brosse à dents électrique Oral-B")
        String offerName,

        @Schema(description = "Prix total de l'offre", example = "29.99", minimum = "0")
        double price,

        @Schema(description = "Devise ISO 4217", example = "EUR", allowableValues = {"EUR", "USD", "GBP"})
        String currency,

        @Schema(description = "URL de redirection vers l'offre commerçante", example = "https://example.com/product/12345")
        String url
) {}
