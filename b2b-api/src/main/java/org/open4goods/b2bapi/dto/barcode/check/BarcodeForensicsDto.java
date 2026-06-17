package org.open4goods.b2bapi.dto.barcode.check;

import io.swagger.v3.oas.annotations.media.Schema;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Gs1Class;

/**
 * Public forensic metadata derived from a barcode string.
 *
 * @param valid whether the check digit is valid
 * @param type detected barcode symbology type
 * @param gs1Prefix 3-digit GS1 company prefix (first 3 digits of the GTIN)
 * @param issuingCountryCode ISO 3166-1 alpha-2 code of the GS1 member organisation (not country of manufacture)
 * @param issuingCountryName human-readable name of the GS1 member country
 * @param flagUrl URL of a small country-flag image for the issuing GS1 member country
 * @param gs1Class product or numbering class derived from the GS1 prefix range
 * @param gs1ClassLabel human-readable description of the GS1 class
 * @param packagingIndicator first digit of a GTIN-14 (1-8 = standard trade unit, 9 = variable measure); null for shorter GTINs
 * @param isbnRegistrationGroup registration group element of an ISBN-13; null for non-book GTINs
 * @param normalizedGtin14 zero-padded 14-digit canonical form
 * @param normalizedGtin13 13-digit form; null when the GTIN-14 indicator digit is non-zero
 * @param checkDigit final check digit of the input barcode
 */
@Schema(description = "Forensic metadata derived from a barcode string.")
public record BarcodeForensicsDto(

        @Schema(description = "Whether the check digit is valid.", example = "true")
        boolean valid,

        @Schema(description = "Detected barcode symbology type.", example = "GTIN_13")
        BarcodeType type,

        @Schema(description = "3-digit GS1 company prefix (first 3 digits of the GTIN).", example = "301", nullable = true)
        String gs1Prefix,

        @Schema(description = "ISO 3166-1 alpha-2 code of the GS1 member organisation. Note: this is the issuing GS1 authority, not necessarily the country of manufacture.", example = "FR", nullable = true)
        String issuingCountryCode,

        @Schema(description = "Human-readable name of the GS1 member country.", example = "France", nullable = true)
        String issuingCountryName,

        @Schema(description = "Flag image URL for the issuing GS1 member country.", example = "/images/flags/FR.webp", nullable = true)
        String flagUrl,

        @Schema(description = "GS1 product or numbering class derived from the prefix range.", example = "GTIN", nullable = true)
        Gs1Class gs1Class,

        @Schema(description = "Human-readable description of the GS1 class.", example = "Standard trade item", nullable = true)
        String gs1ClassLabel,

        @Schema(description = "First digit of a GTIN-14 (1-8 = trade unit, 9 = variable measure). Null for shorter GTINs.", example = "null", nullable = true)
        Integer packagingIndicator,

        @Schema(description = "Registration group element of an ISBN-13. Null for non-book GTINs.", example = "2", nullable = true)
        String isbnRegistrationGroup,

        @Schema(description = "Zero-padded 14-digit canonical form of the GTIN.", example = "03017620422003", nullable = true)
        String normalizedGtin14,

        @Schema(description = "13-digit form of the GTIN. Null when the GTIN-14 indicator digit is non-zero.", example = "3017620422003", nullable = true)
        String normalizedGtin13,

        @Schema(description = "Final check digit of the input barcode.", example = "3", nullable = true)
        Integer checkDigit) {
}
