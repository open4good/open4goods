package org.open4goods.b2bapi.dto.playground;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request structure for the playground price API proxy")
public record PlaygroundRequest(
        @NotNull(message = "apiKeyId is required")
        @Schema(description = "UUID of the API key to use for simulation", example = "5ea8b9cc-c4d6-4444-ac6b-9c7161b36fa1")
        UUID apiKeyId,

        @NotBlank(message = "gtin is required")
        @Schema(description = "Product GTIN to query", example = "0885909950805")
        String gtin,

        @Schema(description = "Locale language", example = "en")
        String language
) {}
