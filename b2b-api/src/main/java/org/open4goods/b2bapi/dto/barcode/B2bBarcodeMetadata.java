package org.open4goods.b2bapi.dto.barcode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Barcode metadata payload to embed directly inside the generated image.")
public record B2bBarcodeMetadata(
    @Schema(description = "Copyright notice to inject into the image file.", example = "Copyright 2026 open4goods")
    String copyright,
    @Schema(description = "Author/origin info to inject into the image file.", example = "Open4Goods B2B API")
    String author,
    @Schema(description = "Description to inject into the image file.", example = "Product GTIN Barcode")
    String description
) {
}
