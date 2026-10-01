package org.open4goods.api.services.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.port.ScanCursor;

/**
 * Verifies checkpoint-based completion scheduling never needs a {@code Product} (AC6).
 */
class FeedIngestionCheckpointServiceTest {

    private static final String OWNER = "merchant-feed-v1";
    private static final SourceId SOURCE_ID = new SourceId("merchant.acme");

    @Test
    void firstAdvanceCreatesACheckpointAtRevisionOne() {
        InMemoryCheckpoints store = new InMemoryCheckpoints();
        FeedIngestionCheckpointService service = new FeedIngestionCheckpointService(store);
        Instant completedAt = Instant.parse("2026-10-01T12:00:00Z");

        boolean stored = service.advance(OWNER, SOURCE_ID, Optional.of(new ScanCursor("row-42")), completedAt);

        assertThat(stored).isTrue();
        IngestionCheckpoint checkpoint = store.find(OWNER, SOURCE_ID).orElseThrow();
        assertThat(checkpoint.revision()).isEqualTo(1L);
        assertThat(checkpoint.updatedAt()).isEqualTo(completedAt);
        assertThat(checkpoint.cursor()).contains(new ScanCursor("row-42"));
    }

    @Test
    void aSecondAdvanceReplacesTheFirstByExpectedRevision() {
        InMemoryCheckpoints store = new InMemoryCheckpoints();
        FeedIngestionCheckpointService service = new FeedIngestionCheckpointService(store);
        service.advance(OWNER, SOURCE_ID, Optional.of(new ScanCursor("row-1")), Instant.parse("2026-10-01T00:00:00Z"));

        boolean stored = service.advance(OWNER, SOURCE_ID, Optional.empty(), Instant.parse("2026-10-01T01:00:00Z"));

        assertThat(stored).isTrue();
        IngestionCheckpoint checkpoint = store.find(OWNER, SOURCE_ID).orElseThrow();
        assertThat(checkpoint.revision()).isEqualTo(2L);
        assertThat(checkpoint.cursor()).isEmpty();
    }

    /** Minimal in-memory double, mirroring the store contract's compare-and-set semantics. */
    private static final class InMemoryCheckpoints implements IngestionCheckpointStore {
        private final Map<String, IngestionCheckpoint> byKey = new HashMap<>();

        @Override
        public Optional<IngestionCheckpoint> find(String owner, SourceId sourceId) {
            return Optional.ofNullable(byKey.get(owner + "/" + sourceId.value()));
        }

        @Override
        public boolean compareAndSet(IngestionCheckpoint checkpoint, long expectedRevision) {
            String key = checkpoint.owner() + "/" + checkpoint.sourceId().value();
            IngestionCheckpoint current = byKey.get(key);
            long currentRevision = current == null ? 0L : current.revision();
            if (currentRevision != expectedRevision) {
                return false;
            }
            byKey.put(key, checkpoint);
            return true;
        }
    }
}
