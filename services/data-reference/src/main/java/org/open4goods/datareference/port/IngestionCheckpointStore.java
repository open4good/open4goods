package org.open4goods.datareference.port;

import java.util.Optional;

import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.SourceId;

/**
 * Store for importer-owned progress, intentionally separate from source heads and offer events.
 */
public interface IngestionCheckpointStore {

    /**
     * Reads one importer's checkpoint for a source.
     *
     * @param owner importer identity
     * @param sourceId source being ingested
     * @return checkpoint when the importer has persisted progress
     */
    Optional<IngestionCheckpoint> find(String owner, SourceId sourceId);

    /**
     * Replaces progress only when the prior checkpoint revision still matches.
     *
     * @param checkpoint new progress
     * @param expectedRevision expected store revision, or zero for initial creation
     * @return {@code true} when the checkpoint was stored
     */
    boolean compareAndSet(IngestionCheckpoint checkpoint, long expectedRevision);
}
