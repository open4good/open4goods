package org.open4goods.api.services.datareference;

import java.time.Instant;
import java.util.Objects;

import org.open4goods.datareference.port.SourceRecordExpirySweeper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Periodically makes elapsed source-record validity visible to product projections.
 *
 * <p>The store retains the expired head and its transition journal. This service only queues the
 * GTIN projections that must be recomputed without that head.
 */
@Service
public class SourceRecordExpirySchedulingService {

    private final SourceRecordExpirySweeper expirySweeper;

    /**
     * Creates the scheduled expiry worker.
     *
     * @param expirySweeper durable expiry work producer
     */
    public SourceRecordExpirySchedulingService(SourceRecordExpirySweeper expirySweeper) {
        this.expirySweeper = Objects.requireNonNull(expirySweeper, "expirySweeper must not be null");
    }

    /**
     * Enqueues expired GTIN attachments every fifteen minutes after the application has settled.
     */
    @Scheduled(initialDelayString = "${open4goods.source-record.expiry-initial-delay:PT5M}",
            fixedDelayString = "${open4goods.source-record.expiry-sweep-interval:PT15M}")
    public void enqueueExpiredHeads() {
        expirySweeper.enqueueExpired(Instant.now());
    }
}
