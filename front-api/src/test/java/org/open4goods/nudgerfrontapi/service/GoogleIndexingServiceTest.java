package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.nudgerfrontapi.config.properties.CacheProperties;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexingProperties;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexingQueueRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

class GoogleIndexingServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void enqueuePathTriggersRealtimeProcessing() {
        GoogleIndexingProperties properties = buildProperties();
        properties.setRealtimeEnabled(true);
        RecordingClient client = new RecordingClient(true);
        GoogleIndexingQueueRepository repository = createRepository(properties);
        GoogleIndexingService service = new GoogleIndexingService(properties, repository, client);

        service.enqueuePath("/products/example");

        GoogleIndexingQueueRepository.GoogleIndexingQueueSnapshot snapshot = repository.snapshot();
        assertThat(snapshot.pendingItems()).isEmpty();
        assertThat(snapshot.indexedUrls()).containsKey("https://example.org/products/example");
        assertThat(client.publishedUrls).containsExactly("https://example.org/products/example");
    }

    @Test
    void processQueueMovesFailedItemsToDeadLetter() {
        GoogleIndexingProperties properties = buildProperties();
        properties.setRealtimeEnabled(false);
        properties.setMaxAttempts(1);
        RecordingClient client = new RecordingClient(false);
        GoogleIndexingQueueRepository repository = createRepository(properties);
        GoogleIndexingService service = new GoogleIndexingService(properties, repository, client);

        repository.enqueue("https://example.org/products/fail", Duration.ofDays(1));
        service.processQueueOnce();

        GoogleIndexingQueueRepository.GoogleIndexingQueueSnapshot snapshot = repository.snapshot();
        assertThat(snapshot.pendingItems()).isEmpty();
        assertThat(snapshot.deadLetterUrls()).containsKey("https://example.org/products/fail");
    }

    private GoogleIndexingQueueRepository createRepository(GoogleIndexingProperties properties) {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setPath(tempDir.toString());
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return new GoogleIndexingQueueRepository(cacheProperties, properties, mapper, Clock.systemUTC());
    }

    private GoogleIndexingProperties buildProperties() {
        GoogleIndexingProperties properties = new GoogleIndexingProperties();
        properties.setEnabled(true);
        properties.setSiteBaseUrl("https://example.org");
        properties.setBatchSize(10);
        properties.setRetryDelay(Duration.ZERO);
        properties.setHistoryRetention(Duration.ofDays(1));
        properties.setQueueFileName("queue.json");
        return properties;
    }

    /**
     * Test client that records published URLs.
     */
    private static final class RecordingClient implements GoogleIndexingClient {

        private final boolean success;
        private final List<String> publishedUrls = new ArrayList<>();

        private RecordingClient(boolean success) {
            this.success = success;
        }

        @Override
        public boolean publish(String url) {
            publishedUrls.add(url);
            return success;
        }
    }
}
