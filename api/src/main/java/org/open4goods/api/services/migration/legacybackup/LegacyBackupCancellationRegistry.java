package org.open4goods.api.services.migration.legacybackup;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-process cancellation flags for running {@code APPLY} operations.
 *
 * <p>One JVM runs at most one importer, so an in-memory registry (rather than a filesystem
 * sentinel, as the standalone Python migration scripts use) is sufficient here: the running
 * {@code apply()} loop checks its dataset's flag between batches and stops cleanly.
 */
public final class LegacyBackupCancellationRegistry {

    private final ConcurrentHashMap<String, AtomicBoolean> flags = new ConcurrentHashMap<>();

    /**
     * Requests that a running import for {@code datasetId} stop after its current batch.
     *
     * @param datasetId dataset to cancel
     */
    public void requestCancellation(String datasetId) {
        flags.computeIfAbsent(datasetId, ignored -> new AtomicBoolean()).set(true);
    }

    /**
     * Reports whether cancellation was requested for {@code datasetId}.
     *
     * @param datasetId dataset to check
     * @return {@code true} when {@link #requestCancellation} was called and not yet cleared
     */
    public boolean isCancellationRequested(String datasetId) {
        AtomicBoolean flag = flags.get(datasetId);
        return flag != null && flag.get();
    }

    /**
     * Clears a dataset's cancellation flag, typically at the start of a fresh {@code APPLY} run.
     *
     * @param datasetId dataset to clear
     */
    public void clear(String datasetId) {
        Objects.requireNonNull(datasetId, "datasetId must not be null");
        flags.remove(datasetId);
    }
}
