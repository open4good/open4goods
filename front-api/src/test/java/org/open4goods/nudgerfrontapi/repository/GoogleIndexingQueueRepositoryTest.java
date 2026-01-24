package org.open4goods.nudgerfrontapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.nudgerfrontapi.config.properties.CacheProperties;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexingProperties;
import org.open4goods.nudgerfrontapi.model.GoogleIndexingQueueItem;

import com.fasterxml.jackson.databind.ObjectMapper;

class GoogleIndexingQueueRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void enqueueDeduplicatesUrls() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        GoogleIndexingQueueRepository repository = createRepository(clock);

        boolean first = repository.enqueue("https://example.org/products/1", Duration.ofDays(1));
        boolean second = repository.enqueue("https://example.org/products/1", Duration.ofDays(1));

        GoogleIndexingQueueRepository.GoogleIndexingQueueSnapshot snapshot = repository.snapshot();
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(snapshot.pendingItems()).hasSize(1);
    }

    @Test
    void reserveBatchHonorsRetryDelayAndAttempts() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        GoogleIndexingQueueRepository repository = createRepository(clock);
        repository.enqueue("https://example.org/products/1", Duration.ofDays(1));

        List<GoogleIndexingQueueItem> firstBatch = repository.reserveBatch(5, Duration.ofMinutes(30), 3, Duration.ofDays(1));
        assertThat(firstBatch).hasSize(1);
        assertThat(firstBatch.get(0).attemptCount()).isEqualTo(1);

        List<GoogleIndexingQueueItem> secondBatch = repository.reserveBatch(5, Duration.ofMinutes(30), 3, Duration.ofDays(1));
        assertThat(secondBatch).isEmpty();

        clock.advance(Duration.ofMinutes(31));
        List<GoogleIndexingQueueItem> thirdBatch = repository.reserveBatch(5, Duration.ofMinutes(30), 3, Duration.ofDays(1));
        assertThat(thirdBatch).hasSize(1);
        assertThat(thirdBatch.get(0).attemptCount()).isEqualTo(2);
    }

    @Test
    void markFailureMovesToDeadLetterAfterMaxAttempts() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        GoogleIndexingQueueRepository repository = createRepository(clock);
        repository.enqueue("https://example.org/products/1", Duration.ofDays(1));

        List<GoogleIndexingQueueItem> batch = repository.reserveBatch(5, Duration.ZERO, 1, Duration.ofDays(1));
        repository.markFailure(batch.get(0), 1);

        GoogleIndexingQueueRepository.GoogleIndexingQueueSnapshot snapshot = repository.snapshot();
        assertThat(snapshot.pendingItems()).isEmpty();
        assertThat(snapshot.deadLetterUrls()).containsKey("https://example.org/products/1");
    }

    private GoogleIndexingQueueRepository createRepository(Clock clock) {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setPath(tempDir.toString());
        GoogleIndexingProperties properties = new GoogleIndexingProperties();
        properties.setQueueFileName("queue.json");
        properties.setSiteBaseUrl("https://example.org");
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return new GoogleIndexingQueueRepository(cacheProperties, properties, mapper, clock);
    }

    /**
     * Mutable clock for testing time-dependent logic.
     */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
