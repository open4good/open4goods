package org.open4goods.nudgerfrontapi.service;

import java.time.Instant;
import java.util.List;

import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexationProperties;
import org.open4goods.nudgerfrontapi.dto.indexation.GoogleIndexationMetricsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueEntry;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueEntry.Status;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service responsible for managing the Google Indexation queue.
 */
@Service
public class GoogleIndexationQueueService {

    private final GoogleIndexationQueueRepository repository;
    private final GoogleIndexationProperties properties;
    private final MeterRegistry meterRegistry;

    private volatile Instant lastSuccessAt;
    private volatile Instant lastFailureAt;
    private volatile String lastFailureMessage;

    /**
     * Create the queue service.
     *
     * @param repository queue repository
     * @param properties indexation configuration
     * @param meterRegistry meter registry for metrics
     */
    public GoogleIndexationQueueService(GoogleIndexationQueueRepository repository,
            GoogleIndexationProperties properties,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.meterRegistry.gauge("google.indexation.queue.size", repository, GoogleIndexationQueueRepository::size);
    }

    /**
     * Enqueue a URL for indexation.
     *
     * @param url product URL
     * @param gtin product identifier
     * @return stored queue entry or {@code null} when input is invalid
     */
    public GoogleIndexationQueueEntry enqueue(String url, long gtin) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        GoogleIndexationQueueEntry entry = repository.upsert(url, gtin);
        if (entry.getStatus() == Status.SUCCESS) {
            return entry;
        }
        entry.setStatus(Status.PENDING);
        return entry;
    }

    /**
     * Claim a batch of pending entries for processing.
     *
     * @param max batch size
     * @return list of claimed entries
     */
    public List<GoogleIndexationQueueEntry> claimPending(int max) {
        List<GoogleIndexationQueueEntry> entries = repository.findPending(max, properties.getMaxAttempts());
        for (GoogleIndexationQueueEntry entry : entries) {
            entry.setStatus(Status.IN_PROGRESS);
            entry.setLastAttemptAt(Instant.now());
            entry.setAttempts(entry.getAttempts() + 1);
        }
        return entries;
    }

    /**
     * Mark a queued URL as successfully processed.
     *
     * @param entry queue entry
     */
    public void markSuccess(GoogleIndexationQueueEntry entry) {
        if (entry == null) {
            return;
        }
        entry.setStatus(Status.SUCCESS);
        entry.setLastSuccessAt(Instant.now());
        entry.setLastError(null);
        lastSuccessAt = entry.getLastSuccessAt();
        meterRegistry.counter("google.indexation.queue.success").increment();
    }

    /**
     * Mark a queued URL as failed.
     *
     * @param entry queue entry
     * @param errorMessage error message to record
     */
    public void markFailure(GoogleIndexationQueueEntry entry, String errorMessage) {
        if (entry == null) {
            return;
        }
        entry.setStatus(Status.FAILED);
        entry.setLastError(errorMessage);
        lastFailureAt = Instant.now();
        lastFailureMessage = errorMessage;
        meterRegistry.counter("google.indexation.queue.failure").increment();
    }

    /**
     * Build the current queue metrics snapshot.
     *
     * @param domainLanguage requested domain language (reserved for future use)
     * @return metrics DTO
     */
    public GoogleIndexationMetricsDto metricsSnapshot(DomainLanguage domainLanguage) {
        List<GoogleIndexationQueueEntry> entries = repository.findAll();
        int pendingCount = (int) entries.stream()
                .filter(entry -> entry.getStatus() == Status.PENDING || entry.getStatus() == Status.IN_PROGRESS)
                .count();
        int failedCount = (int) entries.stream()
                .filter(entry -> entry.getStatus() == Status.FAILED)
                .count();

        return new GoogleIndexationMetricsDto(
                properties.isEnabled(),
                entries.size(),
                pendingCount,
                failedCount,
                lastSuccessAt,
                lastFailureAt,
                lastFailureMessage);
    }

    /**
     * Return the most recent failure message.
     *
     * @return last failure message
     */
    public String getLastFailureMessage() {
        return lastFailureMessage;
    }

    /**
     * Return the most recent success timestamp.
     *
     * @return last success timestamp
     */
    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    /**
     * Return the most recent failure timestamp.
     *
     * @return last failure timestamp
     */
    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    /**
     * Check whether an entry is eligible for retry.
     *
     * @param entry queue entry
     * @return {@code true} when the entry can be retried
     */
    public boolean canRetry(GoogleIndexationQueueEntry entry) {
        return entry != null
                && entry.getStatus() == Status.FAILED
                && entry.getAttempts() < properties.getMaxAttempts();
    }

    /**
     * Return the current queue size.
     *
     * @return size of the queue
     */
    public int getQueueSize() {
        return repository.size();
    }
}
