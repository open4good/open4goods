package org.open4goods.b2bapi.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Finished-request analytics payload emitted to the Redis usage stream.
 *
 * @param organizationId organization id
 * @param apiKeyId API key id
 * @param facetId billed facet id
 * @param gtin requested GTIN
 * @param requestId request id
 * @param httpStatus response status
 * @param billable whether credits were consumed
 * @param creditsConsumed credits consumed
 * @param noPayReason optional no-pay reason
 * @param responseTimeMs response time in milliseconds
 * @param timestamp event timestamp
 */
public record UsageStreamEvent(
        UUID organizationId,
        UUID apiKeyId,
        String facetId,
        String gtin,
        String requestId,
        int httpStatus,
        boolean billable,
        long creditsConsumed,
        String noPayReason,
        Integer responseTimeMs,
        Instant timestamp) {
}
