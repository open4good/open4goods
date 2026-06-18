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

/**
 * Admin endpoints for querying affiliation partners, programs, promotions and transactions.
 * <p>
 * The {@code GET /partners} path is also consumed by {@code front-api} via
 * {@code AffiliationPartnerService} — do not rename it without updating that client.
 * </p>
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class AffiliationController {

    private final FeedService feedService;

    public AffiliationController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/partners")
    @Operation(summary = "Show affiliation partners from all active providers")
    public Set<AffiliationPartner> partners(@RequestParam(required = false) final String provider) {
        return feedService.getPartners(provider);
    }

    @GetMapping("/affiliation/programs")
    @Operation(summary = "Get normalized programs from all active affiliation providers")
    public Collection<AffiliationProgram> programs(@RequestParam(required = false) final String provider) {
        return feedService.getPrograms(provider);
    }

    @GetMapping("/affiliation/promotions")
    @Operation(summary = "Get normalized promotions from all active affiliation providers")
    public Collection<AffiliationPromotion> promotions(@RequestParam(required = false) final String provider) {
        return feedService.getPromotions(provider);
    }

    @GetMapping("/affiliation/transactions")
    @Operation(summary = "Get normalized transactions from all active affiliation providers")
    public Collection<AffiliationTransaction> transactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant to,
            @RequestParam(required = false) final String provider) {
        return feedService.getTransactions(from, to, provider);
    }

    @GetMapping("/affiliation/tracking-link")
    @Operation(summary = "Build a tracking link for a specific provider and program")
    public String trackingLink(
            @RequestParam final String provider,
            @RequestParam final String programId,
            @RequestParam final String targetUrl,
            @RequestParam(required = false) final Map<String, String> subIds) {
        return feedService.buildTrackingLink(provider, programId, targetUrl, subIds);
    }
}
