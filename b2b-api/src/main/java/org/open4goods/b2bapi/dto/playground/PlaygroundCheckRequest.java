package org.open4goods.b2bapi.dto.playground;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request structure for the playground barcode check proxy.
 */
@Schema(description = "Request structure for the playground barcode check proxy")
public record PlaygroundCheckRequest(

        @NotNull(message = "apiKeyId is required")
        @Schema(description = "UUID of the API key to use for the check", example = "5ea8b9cc-c4d6-4444-ac6b-9c7161b36fa1")
        UUID apiKeyId,

        @NotBlank(message = "barcode is required")
        @Schema(description = "Raw barcode string to check", example = "3017620422003")
        String barcode) {
}
