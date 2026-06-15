package org.open4goods.b2bapi.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import org.open4goods.b2bapi.model.OrganizationStatus;

@Schema(description = "Detailed organization metadata for administration")
public record AdminOrganizationDto(
        @Schema(description = "Organization UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Organization name", example = "ACME Corporation")
        String name,

        @Schema(description = "Organization unique slug", example = "acme-corporation")
        String slug,

        @Schema(description = "Billing email address", example = "billing@acme.com")
        String billingEmail,

        @Schema(description = "Default interface language", example = "en")
        String defaultLanguage,

        @Schema(description = "Organization lifecycle status", example = "ACTIVE")
        OrganizationStatus status,

        @Schema(description = "Whether the initial free grant has been applied", example = "true")
        boolean freeGrantApplied,

        @Schema(description = "Authoritative remaining credit balance", example = "2495")
        long creditBalance,

        @Schema(description = "Timestamp of creation", example = "2026-06-15T18:00:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp of last update", example = "2026-06-15T18:30:00Z")
        Instant updatedAt
) {}
