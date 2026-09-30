package org.open4goods.datareference.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.port.ScanCursor;

/**
 * Durable progress owned by one importer, separate from source assertions and price observations.
 *
 * @param owner bounded importer identity, such as {@code eprel-catalogue-v1}
 * @param sourceId source being ingested
 * @param cursor opaque provider or replay position, empty before the first page
 * @param retryCount consecutive retry count
 * @param nextRetryAt scheduled retry instant, empty when no retry is pending
 * @param updatedAt last successful checkpoint update
 * @param revision store-assigned optimistic-lock revision, zero before initial persistence
 */
public record IngestionCheckpoint(
        String owner,
        SourceId sourceId,
        Optional<ScanCursor> cursor,
        int retryCount,
        Optional<Instant> nextRetryAt,
        Instant updatedAt,
        long revision) {

    /** Validates a bounded owner and retry state. */
    public IngestionCheckpoint {
        if (owner == null || !owner.matches("[a-z0-9][a-z0-9._-]{0,119}")) {
            throw new IllegalArgumentException("owner must be a lower-case bounded identifier");
        }
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(cursor, "cursor must not be null");
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        Objects.requireNonNull(nextRetryAt, "nextRetryAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        if (retryCount == 0 && nextRetryAt.isPresent()) {
            throw new IllegalArgumentException("a checkpoint without retries must not schedule a retry");
        }
    }

    /**
     * Compatibility constructor for an unsaved checkpoint.
     *
     * @param owner bounded importer identity
     * @param sourceId source being ingested
     * @param cursor opaque provider or replay position
     * @param retryCount consecutive retry count
     * @param nextRetryAt scheduled retry instant
     * @param updatedAt last successful checkpoint update
     */
    public IngestionCheckpoint(
            String owner,
            SourceId sourceId,
            Optional<ScanCursor> cursor,
            int retryCount,
            Optional<Instant> nextRetryAt,
            Instant updatedAt) {
        this(owner, sourceId, cursor, retryCount, nextRetryAt, updatedAt, 0);
    }
}
