package org.open4goods.api.services.migration.legacybackup;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.port.IngestionCheckpointStore;

/** Minimal in-memory {@link IngestionCheckpointStore} test double with real optimistic locking. */
final class InMemoryIngestionCheckpointStore implements IngestionCheckpointStore {

    private final Map<String, IngestionCheckpoint> stored = new ConcurrentHashMap<>();

    @Override
    public synchronized Optional<IngestionCheckpoint> find(String owner, SourceId sourceId) {
        return Optional.ofNullable(stored.get(key(owner, sourceId)));
    }

    @Override
    public synchronized boolean compareAndSet(IngestionCheckpoint checkpoint, long expectedRevision) {
        String key = key(checkpoint.owner(), checkpoint.sourceId());
        IngestionCheckpoint current = stored.get(key);
        long currentRevision = current == null ? 0 : current.revision();
        if (currentRevision != expectedRevision) {
            return false;
        }
        IngestionCheckpoint updated = new IngestionCheckpoint(checkpoint.owner(), checkpoint.sourceId(),
                checkpoint.cursor(), checkpoint.retryCount(), checkpoint.nextRetryAt(),
                checkpoint.updatedAt() == null ? Instant.now() : checkpoint.updatedAt(), currentRevision + 1);
        stored.put(key, updated);
        return true;
    }

    private String key(String owner, SourceId sourceId) {
        return owner + "::" + sourceId.value();
    }
}
