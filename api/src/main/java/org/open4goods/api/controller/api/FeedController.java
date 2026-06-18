package org.open4goods.api.controller.api;

import org.open4goods.api.services.BatchService;
import org.open4goods.commons.model.dto.api.IndexationResponse;
import org.open4goods.services.feedservice.service.FeedIndexingService;
import org.open4goods.model.RolesConstants;
import org.open4goods.services.feedservice.service.FeedService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for triggering feed fetching and monitoring the indexation queue.
 * Affiliation program/promotion/transaction data is in {@link AffiliationController}.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class FeedController {

    private final FeedIndexingService feedIndexingService;
    private final BatchService batchService;
    private final FeedService feedService;

    public FeedController(BatchService batchService, FeedIndexingService feedIndexingService, FeedService feedService) {
        this.batchService = batchService;
        this.feedIndexingService = feedIndexingService;
        this.feedService = feedService;
    }

    @PostMapping("/feeds")
    @Operation(summary = "Manually run the indexation of all feeds")
    public IndexationResponse indexFeeds(@RequestParam(required = false) final String provider) {
        batchService.fetchFeeds(provider);
        return new IndexationResponse();
    }

    @GetMapping("/feed/queue")
    @Operation(summary = "Show feeds awaiting indexation")
    public Object[] getQueue() {
        return feedIndexingService.getQueue().toArray();
    }

    @PostMapping("/feeds/by-key")
    @Operation(summary = "Run feeds matching the given feed key")
    public void feedByKey(@RequestParam @NotBlank final String feedKey,
            @RequestParam(required = false) final String provider) {
        batchService.fetchFeedsByKey(feedKey, provider);
    }

    @PostMapping("/feeds/by-datasource")
    @Operation(summary = "Manually run the indexation of feeds matching the datasource/provider name")
    public void feedByDatasourceName(@RequestParam @NotBlank final String datasourceName,
            @RequestParam(required = false) final String provider) {
        batchService.fetchFeedsByDatasourceName(datasourceName, provider);
    }

    @PostMapping("/feeds/by-url")
    @Operation(summary = "Run feeds matching the given URL")
    public void feedByUrl(@RequestParam @NotBlank final String url,
            @RequestParam(required = false) final String provider) {
        batchService.fetchFeedsByUrl(url, provider);
    }
}
