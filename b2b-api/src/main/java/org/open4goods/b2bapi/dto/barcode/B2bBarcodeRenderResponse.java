package org.open4goods.b2bapi.dto.barcode;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Response returned for barcode generation request.")
public record B2bBarcodeRenderResponse(
    @Schema(description = "Response metadata.")
    B2bBarcodeRenderMeta meta,
    @Schema(description = "Signed download URL for the generated asset.", example = "https://product-data-api.com/api/v1/barcodes/assets/tok_...")
    String assetUrl,
    @Schema(description = "Asset expiration timestamp.")
    Instant expiresAt,
    @Schema(description = "Dimensions of the generated barcode.")
    B2bBarcodeDimensions dimensions,
    @Schema(description = "Content MIME type.", example = "image/png")
    String contentType,
    @Schema(description = "Warnings produced during rendering.")
    List<String> warnings,
    @Schema(description = "Deterministic hash of inputs for caching validation.", example = "sha256_...")
    String inputHash
) {
}
