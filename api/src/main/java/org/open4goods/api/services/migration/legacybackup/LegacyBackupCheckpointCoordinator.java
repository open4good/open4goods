package org.open4goods.api.services.migration.legacybackup;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.port.IngestionCheckpointStore;

/**
 * Durable, correctness-gating progress for one dataset import.
 *
 * <p>The checkpoint only advances after every mutation in a batch has been reconciled with the
 * target stores (AC3, AC6): a crash mid-batch leaves the prior checkpoint in place, so resuming
 * re-applies at most one batch, which is safe because every write this importer performs is
 * itself idempotent (deterministic ids, {@code storeIfNewer}/CAS semantics).
 */
public final class LegacyBackupCheckpointCoordinator {

    private final IngestionCheckpointStore store;
    private final String owner;

    public LegacyBackupCheckpointCoordinator(IngestionCheckpointStore store, String owner) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
    }

    /**
     * Reads the current position for a dataset.
     *
     * @param datasetId bounded dataset identifier
     * @return current cursor, or {@link LegacyBackupCursor#START} when nothing was committed yet
     */
    public LegacyBackupCursor find(String datasetId) {
        Optional<IngestionCheckpoint> checkpoint = store.find(owner, new SourceId(datasetId));
        return checkpoint.map(value -> LegacyBackupCursor.fromScanCursor(value.cursor())).orElse(LegacyBackupCursor.START);
    }

    /**
     * Reads the raw stored checkpoint, when present, for status reporting.
     *
     * @param datasetId bounded dataset identifier
     * @return stored checkpoint, empty when nothing was committed yet
     */
    public Optional<IngestionCheckpoint> findRaw(String datasetId) {
        return store.find(owner, new SourceId(datasetId));
    }

    /**
     * Advances the checkpoint to {@code next}, retrying the optimistic-lock compare-and-set a
     * bounded number of times against concurrent writers of the same owner/dataset.
     *
     * @param datasetId bounded dataset identifier
     * @param next new position, already fully reconciled with the target stores
     * @throws LegacyBackupInputException when the checkpoint could not be advanced after retrying
     */
    public void advance(String datasetId, LegacyBackupCursor next) {
        SourceId sourceId = new SourceId(datasetId);
        for (int attempt = 0; attempt < 8; attempt++) {
            Optional<IngestionCheckpoint> current = store.find(owner, sourceId);
            long expectedRevision = current.map(IngestionCheckpoint::revision).orElse(0L);
            IngestionCheckpoint updated = new IngestionCheckpoint(
                    owner, sourceId, Optional.of(next.toScanCursor()), 0, Optional.empty(), Instant.now());
            if (store.compareAndSet(updated, expectedRevision)) {
                return;
            }
        }
        throw new LegacyBackupInputException("could not advance checkpoint for dataset: " + datasetId);
    }
}
