package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Metadata returned with every successful Product Data API response.
 *
 * @param requestId request identifier for support and idempotency correlation
 * @param timestamp response generation timestamp
 * @param language resolved response language
 * @param creditsConsumed credits consumed by this request
 * @param creditsRemaining remaining organization credit balance
 * @param billable true when this request consumed credits
 * @param freshnessDays freshness window applied by the endpoint
 * @param responseTimeMs server-side response time in milliseconds
 * @param facets per-facet metering metadata
 * @param coverage per-facet coverage metadata
 */
@Schema(description = "Metadata returned with every successful Product Data API response.")
public record B2bMeta(
        @Schema(description = "Request identifier for support and idempotency correlation.", example = "pdreq_01JZ7V8N9P4K6T2QW3E5R7Y8U9")
        String requestId,
        @Schema(description = "Response generation timestamp.", example = "2026-06-15T10:15:30Z")
        Instant timestamp,
        @Schema(description = "Resolved response language.", example = "en")
        String language,
        @Schema(description = "Credits consumed by this request.", example = "1")
        long creditsConsumed,
        @Schema(description = "Remaining organization credit balance.", example = "249")
        long creditsRemaining,
        @Schema(description = "Whether this request consumed credits.", example = "true")
        boolean billable,
        @Schema(description = "Freshness window applied by the endpoint, in days.", example = "30")
        int freshnessDays,
        @Schema(description = "Server-side response time in milliseconds.", example = "42")
        long responseTimeMs,
        @ArraySchema(schema = @Schema(implementation = B2bFacetMeta.class), arraySchema = @Schema(description = "Per-facet metering metadata."))
        List<B2bFacetMeta> facets,
        @ArraySchema(schema = @Schema(implementation = B2bCoverageMeta.class), arraySchema = @Schema(description = "Per-facet coverage metadata."))
        List<B2bCoverageMeta> coverage) {
}
