package org.open4goods.api.controller.api;

import java.util.Set;

import org.open4goods.api.services.BatchService;
import org.open4goods.commons.model.dto.api.IndexationResponse;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.services.feedservice.service.FeedService;
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


}
