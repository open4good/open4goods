package org.open4goods.b2bapi.dto.barcode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for barcode generation response.")
public record B2bBarcodeRenderMeta(
    @Schema(description = "Unique request identifier.", example = "pdreq_01HF...")
    String requestId,
    @Schema(description = "Whether the request is billable.", example = "true")
    boolean billable,
    @Schema(description = "Credits consumed for the request.", example = "1")
    long creditsConsumed
) {
}
