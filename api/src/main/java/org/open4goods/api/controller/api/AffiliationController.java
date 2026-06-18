package org.open4goods.api.controller.api;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.service.FeedService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints for querying affiliation partners, programs, promotions and transactions.
 * <p>
 * The {@code GET /partners} path is also consumed by {@code front-api} via
 * {@code AffiliationPartnerService} — do not rename it without updating that client.
 * </p>
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Affiliation", description = "Query affiliation partners, programs, promotions and transaction history "
        + "across all configured affiliation providers (Awin, TradeTracker, CJ, Kwanko, etc.).")
public class AffiliationController {

    private final FeedService feedService;

    public AffiliationController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/partners")
    @Operation(
            summary = "List affiliation partners from active providers",
            description = "Returns the deduplicated set of affiliation partners (merchants/advertisers) loaded from "
                    + "all active feed providers. An optional provider filter restricts results to a single network. "
                    + "This endpoint is also called by the front-api AffiliationPartnerService.")
    @ApiResponse(responseCode = "200", description = "Set of affiliation partner records")
    public Set<AffiliationPartner> partners(
            @Parameter(description = "Provider key to filter results (e.g. 'awin', 'tradetracker'). "
                    + "When omitted, results from all active providers are merged.")
            @RequestParam(required = false) final String provider) {
        return feedService.getPartners(provider);
    }

    @GetMapping("/affiliation/programs")
    @Operation(
            summary = "List normalised affiliation programs",
            description = "Returns the normalised affiliate program list from all active providers, "
                    + "or from the specified provider only. Each program corresponds to an advertiser campaign "
                    + "that the platform has joined and that may generate commission on product offers.")
    @ApiResponse(responseCode = "200", description = "Collection of affiliation program records")
    public Collection<AffiliationProgram> programs(
            @Parameter(description = "Provider key to filter results. When omitted, results from all active providers are returned.")
            @RequestParam(required = false) final String provider) {
        return feedService.getPrograms(provider);
    }

    @GetMapping("/affiliation/promotions")
    @Operation(
            summary = "List active affiliation promotions",
            description = "Returns current promotions (discount codes, cashback offers, seasonal deals) "
                    + "advertised by the affiliation programs. Filter by provider to restrict results to a single network.")
    @ApiResponse(responseCode = "200", description = "Collection of affiliation promotion records")
    public Collection<AffiliationPromotion> promotions(
            @Parameter(description = "Provider key to filter results. When omitted, results from all active providers are returned.")
            @RequestParam(required = false) final String provider) {
        return feedService.getPromotions(provider);
    }

    @GetMapping("/affiliation/transactions")
    @Operation(
            summary = "Query affiliation transactions for a time window",
            description = "Fetches commission transactions (clicks, confirmed/pending sales) reported by the affiliation "
                    + "providers for the given date-time range. Both bounds are inclusive and must be supplied as "
                    + "ISO-8601 instants (e.g. 2026-01-01T00:00:00Z). Use the provider parameter to query a single network.")
    @ApiResponse(responseCode = "200", description = "Collection of affiliation transaction records")
    public Collection<AffiliationTransaction> transactions(
            @Parameter(description = "Start of the time window (ISO-8601, e.g. 2026-01-01T00:00:00Z)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant from,
            @Parameter(description = "End of the time window (ISO-8601, e.g. 2026-01-31T23:59:59Z)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant to,
            @Parameter(description = "Provider key to filter results. When omitted, results from all active providers are merged.")
            @RequestParam(required = false) final String provider) {
        return feedService.getTransactions(from, to, provider);
    }

    @GetMapping("/affiliation/tracking-link")
    @Operation(
            summary = "Build an affiliate tracking link",
            description = "Constructs a provider-specific affiliate tracking URL for the given program and target URL. "
                    + "The resulting link routes the user through the affiliate network so that conversions are attributed "
                    + "to the platform account. Optional sub-IDs can be passed for custom attribution tagging.")
    @ApiResponse(responseCode = "200", description = "Fully qualified affiliate tracking URL as a plain string")
    public String trackingLink(
            @Parameter(description = "Provider key (e.g. 'awin', 'tradetracker')", required = true)
            @RequestParam final String provider,
            @Parameter(description = "Affiliate program/advertiser identifier within the network", required = true)
            @RequestParam final String programId,
            @Parameter(description = "Destination URL to wrap with the affiliate tracking link", required = true)
            @RequestParam final String targetUrl,
            @Parameter(description = "Optional map of sub-ID key/value pairs for custom attribution (e.g. campaign, product GTIN)")
            @RequestParam(required = false) final Map<String, String> subIds) {
        return feedService.buildTrackingLink(provider, programId, targetUrl, subIds);
    }
}
