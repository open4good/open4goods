package org.open4goods.api.controller.api;

import java.util.Set;

import org.open4goods.api.services.ScrapperOrchestrationService;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.model.dto.api.IndexationResponse;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.crawler.services.FeedService;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
	
	private final FeedService feedService;
	
	public FeedController( FeedService feedService, CsvDatasourceFetchingService  csvDatasourceFetchingService) {
		this.feedService = feedService;
		this.csvDatasourceFetchingService = csvDatasourceFetchingService;
	}
	
	@PatchMapping(path = "/feeds")
	@Operation(summary="Manualy run the indexation of all feeds")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public IndexationResponse indexFeeds() {
		feedService.fetchFeeds();
		return new IndexationResponse();
	}
	
	@GetMapping(path = "/feeds")
	@Operation(summary="List all feeds from catalogs")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Set<DataSourceProperties> getFeeds() {
		return feedService.getFeedsUrl();
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
		feedService.fetchFeedsByKey(feedKey);
	}
	
	@PatchMapping(path = "/feedsByUrl")
	@Operation(summary="List all feeds from catalogs corresponding the given url")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void feedByUrl(@RequestParam @NotBlank final String url) {
		feedService.fetchFeedsByUrl(url);
	}
	

}
