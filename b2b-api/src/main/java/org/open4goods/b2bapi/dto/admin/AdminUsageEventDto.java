package org.open4goods.b2bapi.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Aggregated request usage event for admin auditing")
public record AdminUsageEventDto(
        @Schema(description = "Event UUID")
        UUID id,
        @Schema(description = "Organization ID associated with the usage")
        UUID organizationId,
        @Schema(description = "Organization name")
        String organizationName,
        @Schema(description = "API key ID if used")
        UUID apiKeyId,
        @Schema(description = "API key prefix if used")
        String apiKeyPrefix,
        @Schema(description = "API facet queried", example = "product.price")
        String facetId,
        @Schema(description = "GTIN queried", example = "0885909950805")
        String gtin,
        @Schema(description = "Prefixed request ID", example = "pdreq_01HF...")
        String requestId,
        @Schema(description = "HTTP response status code", example = "200")
        short httpStatus,
        @Schema(description = "Whether the request was billed", example = "true")
        boolean billable,
        @Schema(description = "Credits consumed by the request", example = "5")
        long creditsConsumed,
        @Schema(description = "Reason why the request was not billed", example = "Product not found")
        String noPayReason,
        @Schema(description = "Query response time in milliseconds", example = "42")
        Integer responseTimeMs,
        @Schema(description = "Timestamp of query execution", example = "2026-06-15T18:22:25Z")
        Instant createdAt
) {}
