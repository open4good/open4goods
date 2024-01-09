package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.open4goods.api.services.FetcherOrchestrationService;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.services.FeedService;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.crawlers.FetcherGlobalStats;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.dto.FetchRequestResponse;
import org.open4goods.model.dto.api.IndexationResponse;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.SerialisationService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;

/**
 * This controller allows informations and communications from fetchers
 * @author goulven
 *
 */
@RestController

public class FeedController {


	private final SerialisationService serialisationService;

	private final FetcherOrchestrationService fetcherOrchestrationService;

	private final DataSourceConfigService datasourceConfigService;

	private final DataFragmentStoreService dataFragmentStoreService;

	private final FeedService feedService;
	
	public FeedController(SerialisationService serialisationService, FetcherOrchestrationService fetcherOrchestrationService, DataSourceConfigService datasourceConfigService, DataFragmentStoreService dataFragmentStoreService, FeedService feedService) {
		this.serialisationService = serialisationService;
		this.fetcherOrchestrationService = fetcherOrchestrationService;
		this.dataFragmentStoreService = dataFragmentStoreService;
		this.datasourceConfigService = datasourceConfigService;
		this.feedService = feedService;
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
	
	@PatchMapping(path = "/feedsByKey")
	@Operation(summary="List all feeds from catalogs corresponding the given field key")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void feedByKey(@RequestBody @NotBlank final String feedKey) {
		feedService.fetchFeedsByKey(feedKey);
	}
	
	@PatchMapping(path = "/feedsByUrl")
	@Operation(summary="List all feeds from catalogs corresponding the given url")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void feedByUrl(@RequestBody @NotBlank final String url) {
		feedService.fetchFeedsByUrl(url);
	}
	

}
