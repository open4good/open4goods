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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for triggering feed fetching and monitoring the indexation queue.
 * Affiliation program/promotion/transaction data is in {@link AffiliationController}.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Feeds", description = "Manually trigger feed fetching and inspect the indexation queue. "
        + "Feeds can be triggered globally or filtered by provider, feed key, datasource name or URL.")
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
    @Operation(
            summary = "Trigger indexation of all feeds",
            description = "Fetches and indexes all registered product feeds from all active providers, "
                    + "or from a single provider when the optional provider parameter is supplied. "
                    + "Returns an IndexationResponse immediately; the actual fetch runs asynchronously.")
    @ApiResponse(responseCode = "200", description = "Feed indexation triggered; response contains request metadata")
    public IndexationResponse indexFeeds(
            @Parameter(description = "Provider key to restrict fetching to a single network (e.g. 'awin', 'tradetracker'). "
                    + "When omitted, all active providers are triggered.")
            @RequestParam(required = false) final String provider) {
        batchService.fetchFeeds(provider);
        return new IndexationResponse();
    }

    @GetMapping("/feed/queue")
    @Operation(
            summary = "Inspect the feed indexation queue",
            description = "Returns the current contents of the in-memory indexation queue — i.e. the feeds that "
                    + "have been fetched and are awaiting processing. Each entry is a pending DataFragment or feed item. "
                    + "Use this to diagnose queue build-up or verify that a fetch completed.")
    @ApiResponse(responseCode = "200", description = "Array of items currently in the indexation queue")
    public Object[] getQueue() {
        return feedIndexingService.getQueue().toArray();
    }

    @PostMapping("/feeds/by-key")
    @Operation(
            summary = "Trigger feeds matching a given feed key",
            description = "Fetches and indexes only the feeds whose key matches the supplied feedKey string. "
                    + "Feed keys are logical identifiers defined in the datasource configuration YAML (e.g. 'rakuten-fr'). "
                    + "Optionally restrict to a single provider.")
    @ApiResponse(responseCode = "200", description = "Matching feeds triggered for indexation")
    public void feedByKey(
            @Parameter(description = "Feed key as defined in the datasource configuration", required = true)
            @RequestParam @NotBlank final String feedKey,
            @Parameter(description = "Provider key to further restrict results (optional)")
            @RequestParam(required = false) final String provider) {
        batchService.fetchFeedsByKey(feedKey, provider);
    }

    @PostMapping("/feeds/by-datasource")
    @Operation(
            summary = "Trigger feeds matching a datasource name",
            description = "Fetches and indexes only the feeds belonging to the datasource/provider whose registered name "
                    + "matches the supplied datasourceName. Useful when a specific provider's configuration has been "
                    + "updated and only those feeds need to be refreshed.")
    @ApiResponse(responseCode = "200", description = "Matching feeds triggered for indexation")
    public void feedByDatasourceName(
            @Parameter(description = "Registered datasource name (as it appears in /datasources)", required = true)
            @RequestParam @NotBlank final String datasourceName,
            @Parameter(description = "Provider key to further restrict results (optional)")
            @RequestParam(required = false) final String provider) {
        batchService.fetchFeedsByDatasourceName(datasourceName, provider);
    }

    @PostMapping("/feeds/by-url")
    @Operation(
            summary = "Trigger feeds matching a URL pattern",
            description = "Fetches and indexes only the feeds whose configured URL matches or contains the supplied "
                    + "url string. Useful for triggering a single known feed when its exact key is not remembered.")
    @ApiResponse(responseCode = "200", description = "Matching feeds triggered for indexation")
    public void feedByUrl(
            @Parameter(description = "URL string (or substring) to match against registered feed URLs", required = true)
            @RequestParam @NotBlank final String url,
            @Parameter(description = "Provider key to further restrict results (optional)")
            @RequestParam(required = false) final String provider) {
        batchService.fetchFeedsByUrl(url, provider);
    }
}
