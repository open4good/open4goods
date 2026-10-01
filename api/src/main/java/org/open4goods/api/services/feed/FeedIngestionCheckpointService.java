package org.open4goods.api.services.feed;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.port.ScanCursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Advances merchant-feed ingestion progress through {@link IngestionCheckpointStore}.
 *
 * <p>This is the completion-scheduling boundary for the reference/offer feed path: a feed run's
 * progress and completion are importer-owned state tracked by {@link IngestionCheckpoint}, never
 * {@code Product.datasourceCodes} or any other field derived from a {@code Product} (AC6). A
 * failure to persist the checkpoint is logged and swallowed, following the same
 * best-effort-diagnostic contract {@code IcecatCompletionService} uses: it never gates whether the
 * feed's reference and offer data were themselves accepted.
 */
@Component
public class FeedIngestionCheckpointService {

    private final IngestionCheckpointStore checkpointStore;

    @Autowired
    public FeedIngestionCheckpointService(IngestionCheckpointStore checkpointStore) {
        this.checkpointStore = Objects.requireNonNull(checkpointStore, "checkpointStore must not be null");
    }

    /**
     * Records that {@code owner} has processed a feed run for {@code sourceId} up to
     * {@code cursor}, compare-and-set against whatever revision is currently stored.
     *
     * @param owner importer identity (stable across runs of the same feed pipeline)
     * @param sourceId source being ingested
     * @param cursor resumable position reached, or {@code empty} when the run completed fully
     * @param completedAt instant this progress was reached
     * @return {@code true} when the checkpoint was stored
     */
    public boolean advance(String owner, SourceId sourceId, Optional<ScanCursor> cursor, Instant completedAt) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(cursor, "cursor must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        Optional<IngestionCheckpoint> current = checkpointStore.find(owner, sourceId);
        long expectedRevision = current.map(IngestionCheckpoint::revision).orElse(0L);
        IngestionCheckpoint next = new IngestionCheckpoint(
                owner, sourceId, cursor, 0, Optional.empty(), completedAt, expectedRevision + 1);
        return checkpointStore.compareAndSet(next, expectedRevision);
    }
}
