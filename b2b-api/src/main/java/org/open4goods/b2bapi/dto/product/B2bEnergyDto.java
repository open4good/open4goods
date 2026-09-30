package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Public, sanitized EU energy label payload sourced from EPREL. Free of charge for any
 * authenticated account (EPREL API Terms and Conditions 4 1(i)/4 2(a)); never billed.
 *
 * @param gtin normalized request GTIN
 * @param energyClass EU energy efficiency class letter (e.g. "A", "G")
 * @param energyClassImage EPREL-hosted energy class image filename, when available
 * @param productGroup EPREL product group code
 * @param eprelCategories EPREL category codes for the model
 * @param modelIdentifier manufacturer model identifier as registered in EPREL
 * @param supplierOrTrademark supplier or trademark name as registered in EPREL
 * @param eprelRegistrationNumber EPREL registration number for the model
 * @param attribution mandatory EPREL attribution block
 */
@Schema(description = "Public, sanitized EU energy label facet sourced from EPREL. Free of charge for any "
        + "authenticated account; never billed.")
public record B2bEnergyDto(
        @Schema(description = "Normalized request GTIN.", example = "0885909950805")
        String gtin,
        @Schema(description = "EU energy efficiency class letter.", example = "A", nullable = true)
        String energyClass,
        @Schema(description = "EPREL-hosted energy class image filename, when available.",
                example = "energy_class_A.png", nullable = true)
        String energyClassImage,
        @Schema(description = "EPREL product group code.", example = "airconditioners", nullable = true)
        String productGroup,
        @Schema(description = "EPREL category codes for the model.", nullable = true)
        List<String> eprelCategories,
        @Schema(description = "Manufacturer model identifier as registered in EPREL.", nullable = true)
        String modelIdentifier,
        @Schema(description = "Supplier or trademark name as registered in EPREL.", nullable = true)
        String supplierOrTrademark,
        @Schema(description = "EPREL registration number for the model.", nullable = true)
        String eprelRegistrationNumber,
        @Schema(description = "Mandatory EPREL attribution block.")
        B2bEprelAttributionDto attribution) {
}
