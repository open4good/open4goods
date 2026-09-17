package org.open4goods.datareference.port;

import java.time.Instant;

/**
 * Enqueues projection removal work for source-record heads whose validity period has elapsed.
 *
 * <p>The sweep does not delete or rewrite a head. Its transition metadata remains available for
 * replay and audit while consumers stop using the expired assertions.
 */
public interface SourceRecordExpirySweeper {

    /**
     * Enqueues each expired head's attached GTINs for projection recomputation.
     *
     * <p>Repeated calls for the same expiry are idempotent. Implementations must process bounded
     * pages rather than materializing the whole source-head catalogue.
     *
     * @param asOf instant at which expiration is evaluated
     * @return number of newly enqueued expired heads
     */
    int enqueueExpired(Instant asOf);
}
