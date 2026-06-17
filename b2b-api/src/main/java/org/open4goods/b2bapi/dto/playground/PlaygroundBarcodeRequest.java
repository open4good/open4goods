package org.open4goods.b2bapi.dto.playground;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderRequest;

@Schema(description = "Request structure for the playground barcode rendering proxy")
public record PlaygroundBarcodeRequest(
        @NotNull(message = "apiKeyId is required")
        @Schema(description = "UUID of the API key to use for rendering", example = "5ea8b9cc-c4d6-4444-ac6b-9c7161b36fa1")
        UUID apiKeyId,

        @NotNull(message = "request is required")
        @Valid
        @Schema(description = "Barcode rendering parameters")
        B2bBarcodeRenderRequest request
) {}
