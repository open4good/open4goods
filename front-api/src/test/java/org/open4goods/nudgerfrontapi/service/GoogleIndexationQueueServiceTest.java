package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexationProperties;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueEntry;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueEntry.Status;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for {@link GoogleIndexationQueueService}.
 */
class GoogleIndexationQueueServiceTest {

    @Test
    void claimPendingMarksEntriesInProgress() {
        GoogleIndexationQueueRepository repository = new GoogleIndexationQueueRepository();
        GoogleIndexationProperties properties = new GoogleIndexationProperties();
        properties.setMaxAttempts(2);
        GoogleIndexationQueueService service = new GoogleIndexationQueueService(repository, properties, new SimpleMeterRegistry());

        GoogleIndexationQueueEntry entry = service.enqueue("https://example.org/product/1", 1L);

        List<GoogleIndexationQueueEntry> pending = service.claimPending(1);

        assertThat(pending).hasSize(1);
        assertThat(entry.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(entry.getAttempts()).isEqualTo(1);
    }

    @Test
    void markSuccessUpdatesStatusAndMetricsSnapshot() {
        GoogleIndexationQueueRepository repository = new GoogleIndexationQueueRepository();
        GoogleIndexationProperties properties = new GoogleIndexationProperties();
        GoogleIndexationQueueService service = new GoogleIndexationQueueService(repository, properties, new SimpleMeterRegistry());

        GoogleIndexationQueueEntry entry = service.enqueue("https://example.org/product/2", 2L);
        service.markSuccess(entry);

        assertThat(entry.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(service.metricsSnapshot(DomainLanguage.fr).queuedCount()).isEqualTo(1);
        assertThat(service.metricsSnapshot(DomainLanguage.fr).failedCount()).isZero();
    }
}
