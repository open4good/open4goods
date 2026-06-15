package org.open4goods.b2bapi.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Schema(description = "Request to manually grant credits to an organization")
public record AdminManualGrantRequest(
        @Schema(description = "Credits to grant", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "Credits must be positive.")
        long credits,

        @Schema(description = "Description/reason for the grant", example = "Service goodwill adjustment", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Note is required.")
        String note,

        @Schema(description = "Optional expiration date", example = "2026-12-31T23:59:59Z")
        Instant expiresAt
) {}
