package org.open4goods.nudgerfrontapi.service;

import java.time.Instant;
import java.util.List;

import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexationProperties;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueEntry;
import org.open4goods.services.googleindexation.dto.GoogleIndexationResult;
import org.open4goods.services.googleindexation.dto.GoogleIndexationResultItem;
import org.open4goods.services.googleindexation.service.GoogleIndexationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service orchestrating real-time and batch Google Indexation dispatches.
 */
@Service
public class GoogleIndexationDispatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIndexationDispatchService.class);

    private final GoogleIndexationProperties properties;
    private final GoogleIndexationQueueService queueService;
    private final GoogleIndexationService googleIndexationService;
    private final ProductUrlService productUrlService;
    private final MeterRegistry meterRegistry;

    /**
     * Create the dispatch service.
     *
     * @param properties indexation configuration
     * @param queueService queue service
     * @param googleIndexationService Google Indexing API client
     * @param productUrlService product URL resolver
     * @param meterRegistry meter registry for metrics
     */
    public GoogleIndexationDispatchService(GoogleIndexationProperties properties,
            GoogleIndexationQueueService queueService,
            GoogleIndexationService googleIndexationService,
            ProductUrlService productUrlService,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.queueService = queueService;
        this.googleIndexationService = googleIndexationService;
        this.productUrlService = productUrlService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Handle a successful review generation event.
     *
     * @param gtin product identifier
     * @param domainLanguage domain language
     */
    public void handleReviewSuccess(long gtin, DomainLanguage domainLanguage) {
        if (!properties.isEnabled() || !googleIndexationService.isEnabled()) {
            return;
        }
        String url = productUrlService.resolveProductUrl(gtin, domainLanguage);
        if (!StringUtils.hasText(url)) {
            LOGGER.warn("Unable to enqueue Google indexation for GTIN {} due to missing URL.", gtin);
            return;
        }
        GoogleIndexationQueueEntry entry = queueService.enqueue(url, gtin);
        if (entry == null) {
            return;
        }
        meterRegistry.counter("google.indexation.queue.enqueued").increment();
        if (properties.isRealtimeEnabled()) {
            dispatchEntry(entry);
        }
    }

    /**
     * Dispatch queued URLs on a fixed schedule.
     */
    @Scheduled(fixedDelayString = "${front.google-indexation.batch-interval:PT30M}")
    public void dispatchBatch() {
        if (!properties.isEnabled() || !properties.isBatchEnabled() || !googleIndexationService.isEnabled()) {
            return;
        }
        List<GoogleIndexationQueueEntry> entries = queueService.claimPending(properties.getBatchSize());
        if (entries.isEmpty()) {
            return;
        }
        List<String> urls = entries.stream().map(GoogleIndexationQueueEntry::getUrl).toList();
        GoogleIndexationResult result = googleIndexationService.publishUrls(urls);
        handleBatchResult(entries, result);
    }

    /**
     * Dispatch a single entry immediately.
     *
     * @param entry queue entry
     */
    private void dispatchEntry(GoogleIndexationQueueEntry entry) {
        GoogleIndexationResultItem resultItem = googleIndexationService.publishUrl(entry.getUrl());
        if (resultItem.success()) {
            queueService.markSuccess(entry);
        } else {
            queueService.markFailure(entry, resultItem.message());
        }
    }

    /**
     * Apply a batch result to the queue entries.
     *
     * @param entries entries dispatched
     * @param result batch result
     */
    private void handleBatchResult(List<GoogleIndexationQueueEntry> entries, GoogleIndexationResult result) {
        if (result == null || result.items() == null) {
            for (GoogleIndexationQueueEntry entry : entries) {
                queueService.markFailure(entry, "Missing result from Google Indexing API");
            }
            return;
        }
        for (GoogleIndexationResultItem item : result.items()) {
            GoogleIndexationQueueEntry entry = entries.stream()
                    .filter(candidate -> candidate.getUrl().equals(item.url()))
                    .findFirst()
                    .orElse(null);
            if (entry == null) {
                continue;
            }
            if (item.success()) {
                queueService.markSuccess(entry);
            } else {
                queueService.markFailure(entry, item.message());
            }
        }
        LOGGER.info("Google indexation batch completed at {} with {} successes and {} failures.",
                Instant.now(), result.successCount(), result.failureCount());
    }
}
