package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.model.product.BarcodeType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Holds information inferred from the product GTIN.
 */
public record ProductGtinInfoDto(
        @Schema(description = "UPC type derived from the GTIN")
        BarcodeType upcType,
        @Schema(description = "Manufacturer country code inferred from the GTIN prefix", example = "FR")
        String countryCode,
        @Schema(description = "Manufacturer country name localised with the requested domainLanguage", example = "France")
        String countryName,
        @Schema(description = "URL of the country flag icon matching the manufacturer country", example = "/images/flags/fr.png")
        String countryFlagUrl
) {
}
