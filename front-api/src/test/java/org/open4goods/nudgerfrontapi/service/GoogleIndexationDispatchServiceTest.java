package org.open4goods.nudgerfrontapi.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexationProperties;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.repository.GoogleIndexationQueueEntry;
import org.open4goods.services.googleindexation.dto.GoogleIndexationResult;
import org.open4goods.services.googleindexation.dto.GoogleIndexationResultItem;
import org.open4goods.services.googleindexation.service.GoogleIndexationService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for {@link GoogleIndexationDispatchService}.
 */
class GoogleIndexationDispatchServiceTest {

    @Test
    void handleReviewSuccessDispatchesImmediatelyWhenEnabled() {
        GoogleIndexationProperties properties = new GoogleIndexationProperties();
        properties.setEnabled(true);
        properties.setRealtimeEnabled(true);

        GoogleIndexationQueueService queueService = mock(GoogleIndexationQueueService.class);
        GoogleIndexationService googleIndexationService = mock(GoogleIndexationService.class);
        ProductUrlService productUrlService = mock(ProductUrlService.class);

        when(googleIndexationService.isEnabled()).thenReturn(true);
        GoogleIndexationQueueEntry entry = new GoogleIndexationQueueEntry("https://example.org/product/1", 1L, java.time.Instant.now());
        when(productUrlService.resolveProductUrl(1L, DomainLanguage.fr)).thenReturn(entry.getUrl());
        when(queueService.enqueue(entry.getUrl(), 1L)).thenReturn(entry);
        when(googleIndexationService.publishUrl(entry.getUrl()))
                .thenReturn(new GoogleIndexationResultItem(entry.getUrl(), true, "ok", java.time.Instant.now()));

        GoogleIndexationDispatchService service = new GoogleIndexationDispatchService(
                properties,
                queueService,
                googleIndexationService,
                productUrlService,
                new SimpleMeterRegistry());

        service.handleReviewSuccess(1L, DomainLanguage.fr);

        verify(queueService).markSuccess(entry);
    }

    @Test
    void dispatchBatchMarksFailures() {
        GoogleIndexationProperties properties = new GoogleIndexationProperties();
        properties.setEnabled(true);
        properties.setBatchEnabled(true);
        properties.setBatchSize(2);

        GoogleIndexationQueueService queueService = mock(GoogleIndexationQueueService.class);
        GoogleIndexationService googleIndexationService = mock(GoogleIndexationService.class);
        ProductUrlService productUrlService = mock(ProductUrlService.class);

        when(googleIndexationService.isEnabled()).thenReturn(true);
        GoogleIndexationQueueEntry entry = new GoogleIndexationQueueEntry("https://example.org/product/2", 2L, java.time.Instant.now());
        when(queueService.claimPending(anyInt())).thenReturn(List.of(entry));
        GoogleIndexationResult result = new GoogleIndexationResult(1, 0, 1, java.time.Instant.now(),
                List.of(new GoogleIndexationResultItem(entry.getUrl(), false, "error", java.time.Instant.now())));
        when(googleIndexationService.publishUrls(List.of(entry.getUrl()))).thenReturn(result);

        GoogleIndexationDispatchService service = new GoogleIndexationDispatchService(
                properties,
                queueService,
                googleIndexationService,
                productUrlService,
                new SimpleMeterRegistry());

        service.dispatchBatch();

        verify(queueService).markFailure(eq(entry), eq("error"));
    }
}
