package org.open4goods.b2bapi.dto.barcode.check;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response envelope for the {@code barcode.check} facet.
 *
 * @param barcode the raw barcode as supplied by the caller
 * @param forensics forensic metadata derived from the barcode
 * @param product optional product teaser when a matching entry is found in the nudger.fr index; null otherwise
 */
@Schema(description = "Result of a barcode validity check and product lookup.")
public record BarcodeCheckResponse(

        @Schema(description = "The raw barcode as supplied by the caller.", example = "3017620422003")
        String barcode,

        @Schema(description = "Forensic metadata derived from the barcode string.")
        BarcodeForensicsDto forensics,

        @Schema(description = "Product teaser when a matching entry is found in the nudger.fr index. Null when the barcode is invalid or the product is not indexed.", nullable = true)
        ProductTeaserDto product) {
}
