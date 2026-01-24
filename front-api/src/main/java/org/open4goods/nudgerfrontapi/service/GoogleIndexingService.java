package org.open4goods.nudgerfrontapi.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexingProperties;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexingQueueRepository;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexingQueueRepository.GoogleIndexingQueueSnapshot;
import org.open4goods.nudgerfrontapi.service.dto.GoogleIndexingMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Orchestrates Google Indexing API submissions, including batching and retries.
 */
@Service
public class GoogleIndexingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIndexingService.class);

    private final GoogleIndexingProperties properties;
    private final GoogleIndexingQueueRepository queueRepository;
    private final GoogleIndexingClient client;
    private final ReentrantLock processingLock = new ReentrantLock();

    /**
     * Create the Google Indexing service.
     *
     * @param properties      configuration properties
     * @param queueRepository repository storing queued URLs
     * @param client          client used to call the Google Indexing API
     */
    public GoogleIndexingService(GoogleIndexingProperties properties,
                                 GoogleIndexingQueueRepository queueRepository,
                                 GoogleIndexingClient client) {
        this.properties = properties;
        this.queueRepository = queueRepository;
        this.client = client;
    }

    /**
     * Enqueue an absolute URL for indexing and optionally process immediately.
     *
     * @param url absolute URL to index
     */
    public void enqueueUrl(String url) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(url)) {
            return;
        }
        boolean enqueued = queueRepository.enqueue(url, properties.getHistoryRetention());
        if (enqueued && properties.isRealtimeEnabled()) {
            processQueueOnce();
        }
    }

    /**
     * Enqueue a URL path by combining it with the configured site base URL.
     *
     * @param path URL path (e.g. "/products/example")
     */
    public void enqueuePath(String path) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        resolveAbsoluteUrl(path).ifPresent(this::enqueueUrl);
    }

    /**
     * Return a snapshot of indexing metrics.
     *
     * @return metrics snapshot
     */
    public GoogleIndexingMetrics metrics() {
        GoogleIndexingQueueSnapshot snapshot = queueRepository.snapshot();
        return new GoogleIndexingMetrics(
                properties.isEnabled(),
                snapshot.pendingItems().size(),
                snapshot.indexedUrls().size(),
                snapshot.deadLetterUrls().size(),
                snapshot.lastSuccessAt(),
                snapshot.lastFailureAt(),
                properties.getBatchSize(),
                properties.getRetryDelay(),
                properties.getMaxAttempts(),
                properties.isRealtimeEnabled());
    }

    /**
     * Execute scheduled batch processing for pending URLs.
     */
    @Scheduled(fixedDelayString = "PT30M")
    public void processQueue() {
        if (!properties.isEnabled()) {
            return;
        }
        processQueueOnce();
    }

    /**
     * Process a single batch of queued URLs.
     */
    public void processQueueOnce() {
        if (!properties.isEnabled()) {
            return;
        }
        if (!processingLock.tryLock()) {
            return;
        }
        try {
            List<org.open4goods.nudgerfrontapi.model.GoogleIndexingQueueItem> batch =
                    queueRepository.reserveBatch(properties.getBatchSize(),
                            properties.getRetryDelay(),
                            properties.getMaxAttempts(),
                            properties.getHistoryRetention());
            if (batch.isEmpty()) {
                return;
            }
            LOGGER.info("Processing {} Google indexing URLs", batch.size());
            for (org.open4goods.nudgerfrontapi.model.GoogleIndexingQueueItem item : batch) {
                boolean success = client.publish(item.url());
                if (success) {
                    queueRepository.markSuccess(item.url());
                } else {
                    queueRepository.markFailure(item, properties.getMaxAttempts());
                }
            }
        } finally {
            processingLock.unlock();
        }
    }

    /**
     * Resolve an absolute URL using the configured base URL.
     *
     * @param path URL path or absolute URL
     * @return resolved absolute URL if configuration is valid
     */
    public Optional<String> resolveAbsoluteUrl(String path) {
        if (!StringUtils.hasText(path)) {
            return Optional.empty();
        }
        String baseUrl = normalizeBaseUrl(properties.getSiteBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            return Optional.empty();
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return Optional.of(path);
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return Optional.of(baseUrl + normalizedPath);
    }

    /**
     * Determine whether the service is correctly configured for use.
     *
     * @return {@code true} when the service can authenticate to Google
     */
    public boolean isConfigured() {
        if (!properties.isEnabled()) {
            return true;
        }
        return StringUtils.hasText(properties.getServiceAccountJson())
                || StringUtils.hasText(properties.getServiceAccountPath());
    }

    /**
     * Determine the age of the last successful submission.
     *
     * @param now current timestamp
     * @return optional duration since last success
     */
    public Optional<Duration> ageSinceLastSuccess(Instant now) {
        GoogleIndexingQueueSnapshot snapshot = queueRepository.snapshot();
        if (snapshot.lastSuccessAt() == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(snapshot.lastSuccessAt(), now));
    }

    /**
     * Normalize the base URL by removing trailing slashes.
     *
     * @param baseUrl configured base URL
     * @return normalized base URL
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
