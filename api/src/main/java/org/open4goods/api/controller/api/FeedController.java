package org.open4goods.api.controller.api;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.open4goods.api.services.BatchService;
import org.open4goods.commons.model.dto.api.IndexationResponse;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.service.FeedService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;

/**
 * This controller allows informations and communications from fetchers
 * @author goulven
 *
 */
@RestController

public class FeedController {


	private final CsvDatasourceFetchingService  csvDatasourceFetchingService;

	private final BatchService batchService;

	private FeedService feedService;

	public FeedController( BatchService batchService, CsvDatasourceFetchingService  csvDatasourceFetchingService, FeedService feedService) {
		this.batchService = batchService;
		this.csvDatasourceFetchingService = csvDatasourceFetchingService;
		this.feedService = feedService;
	}

	@GetMapping(path = "/partners")
	@Operation(summary="Show affiliation partners from feed")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Set<AffiliationPartner> partners() {
		return feedService.getPartners();
	}


	@PatchMapping(path = "/feeds")
	@Operation(summary="Manualy run the indexation of all feeds")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public IndexationResponse indexFeeds() {
		batchService.fetchFeeds();
		return new IndexationResponse();
	}




	@GetMapping(path = "/feed/queue")
	@Operation(summary="Show feeds awaiting indexation")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Object[] getQueue() {
		return csvDatasourceFetchingService.getQueue().toArray();
	}

	@PatchMapping(path = "/feedsByKey")
	@Operation(summary="List all feeds from catalogs corresponding the given field key")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void feedByKey(@RequestParam @NotBlank final String feedKey) {
		batchService.fetchFeedsByKey(feedKey);
	}

    @PatchMapping(path = "/feedsByDatasourceName")
    @Operation(summary="Manually run the indexation of feeds matching the datasource/provider name")
    @PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
    public void feedByDatasourceName(@RequestParam @NotBlank final String datasourceName)
    {
        batchService.fetchFeedsByDatasourceName(datasourceName);
    }

	@PatchMapping(path = "/feedsByUrl")
	@Operation(summary="List all feeds from catalogs corresponding the given url")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void feedByUrl(@RequestParam @NotBlank final String url) {
		batchService.fetchFeedsByUrl(url);
	}

	@GetMapping(path = "/admin/affiliation/programs")
	@Operation(summary="Get normalized programs from all active affiliation providers")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Collection<AffiliationProgram> programs()
	{
		return feedService.getPrograms();
	}

	@GetMapping(path = "/admin/affiliation/promotions")
	@Operation(summary="Get normalized promotions from all active affiliation providers")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Collection<AffiliationPromotion> promotions()
	{
		return feedService.getPromotions();
	}

	@GetMapping(path = "/admin/affiliation/transactions")
	@Operation(summary="Get normalized transactions from all active affiliation providers")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Collection<AffiliationTransaction> transactions(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant to)
	{
		return feedService.getTransactions(from, to);
	}

	@GetMapping(path = "/admin/affiliation/tracking-link")
	@Operation(summary="Build a tracking link for a specific provider and program")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public String trackingLink(
			@RequestParam final String provider,
			@RequestParam final String programId,
			@RequestParam final String targetUrl,
			@RequestParam(required = false) final Map<String, String> subIds)
	{
		return feedService.buildTrackingLink(provider, programId, targetUrl, subIds);
	}
}
