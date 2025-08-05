package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.api.services.ScrapperOrchestrationService;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.model.crawlers.FetcherGlobalStats;
import org.open4goods.commons.model.dto.FetchRequestResponse;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

public class ScraperOrchestrationController {


	private final SerialisationService serialisationService;

	private final ScrapperOrchestrationService fetcherOrchestrationService;

	private final DataSourceConfigService datasourceConfigService;

	private final DataFragmentStoreService dataFragmentStoreService;
	
	public ScraperOrchestrationController(SerialisationService serialisationService, ScrapperOrchestrationService fetcherOrchestrationService, DataSourceConfigService datasourceConfigService, DataFragmentStoreService dataFragmentStoreService) {
		this.serialisationService = serialisationService;
		this.fetcherOrchestrationService = fetcherOrchestrationService;
		this.dataFragmentStoreService = dataFragmentStoreService;
		this.datasourceConfigService = datasourceConfigService;
	}


	
	
	


	@GetMapping(path=UrlConstants.MASTER_API_CRAWLERS)
	@Operation(summary="List all availlable fetchers and their stats")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public Map<String,FetcherGlobalStats> fetcherStats () {
		return fetcherOrchestrationService.getCrawlerStatuses().asMap();
	}

	@PostMapping(path=UrlConstants.MASTER_API_CRAWLERS  + UrlConstants.MASTER_API_CRAWLER_TRIGGER_SUFFIX+"/all")
	@Operation(summary="Run a all datasources retrieving against best availlables node")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public FetchRequestResponse triggerAllFetcher() {
		for (final Entry<String, DataSourceProperties> ds : datasourceConfigService.datasourceConfigs().entrySet()) {
			fetcherOrchestrationService.triggerRemoteCrawling(ds.getKey());
		}
		return new FetchRequestResponse(true);
	}




	@PostMapping(path=UrlConstants.MASTER_API_CRAWLERS  + UrlConstants.MASTER_API_CRAWLER_TRIGGER_SUFFIX+"/{datasourceName}")
	@Operation(summary="Run a datasource retrieving against the best availlable node")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public FetchRequestResponse triggerFetcher( @PathVariable @NotBlank final String datasourceName) {
		return fetcherOrchestrationService.triggerRemoteCrawling(datasourceName);
	}


	@PostMapping(path=UrlConstants.MASTER_API_CRAWLERS  + UrlConstants.MASTER_API_CRAWLER_SYNCH_HTTP_FETCH)
	@Operation(summary="Run an url direct fetching against the best availlable node")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public DataFragment testFetchUrl(  @RequestBody @NotBlank final String url) throws InvalidParameterException, ValidationException{
		// Get the providerName corresponding to the url
		final DataSourceProperties dsp = datasourceConfigService.getDatasourcePropertiesForUrl(url);
		if (null == dsp) {
			throw new InvalidParameterException("Cannot find a matching DatasourceProperties for " + url);
		}
		DataFragment df = fetcherOrchestrationService.triggerHttpSynchFetching(dsp, url);

		dataFragmentStoreService.queueDataFragment(df);

		return df;


	}

	//
	//	@GetMapping(path=UrlConstants.MASTER_API_CRAWLERS  + UrlConstants.MASTER_API_CRAWLER_SYNCH_CSV_FETCH)
	//	@Operation(summary="Run a csv line direct fetching against the best availlable node")
	//	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	//	public DataFragment testFetchCsv(  @RequestParam @NotBlank final String csvLine, @RequestParam @NotBlank final String csvHeaders, @RequestParam @NotBlank final String datasourceName) throws InvalidParameterException{
	//		// Get the providerName corresponding to the url
	//		final DataSourceProperties dsp = this.datasourceConfigService.getDatasourceConfig(datasourceName);
	//		if (null == dsp) {
	//			throw new InvalidParameterException("Cannot find a matching DatasourceProperties for " + datasourceName);
	//		}
	//		return this.fetcherOrchestrationService.triggerCsvSynchFetching(dsp, csvLine, csvHeaders);
	//	}


	@PostMapping(path=UrlConstants.MASTER_API_CRAWLERS  + UrlConstants.MASTER_API_CRAWLER_SYNCH_FETCH_WITH_CONFIG)
	@Operation(summary="Run an url direct fetching against the best availlable node, with a given DataSourceProperties")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public DataFragment fetchUrlWithConfig(  @RequestParam @NotBlank final String url, @RequestBody @NotBlank final String datasourceProperty ) throws InvalidParameterException, JsonParseException, JsonMappingException, IOException, SerialisationException{
		// Get the providerName corresponding to the url
		return fetcherOrchestrationService.triggerHttpSynchFetching( serialisationService.fromYaml(datasourceProperty, DataSourceProperties.class), url);
	}

	@GetMapping(path=UrlConstants.MASTER_API_CRAWLERS+"/{crawlerNodeName}" + UrlConstants.MASTER_API_CRAWLER_TRIGGER_SUFFIX)
	@Operation(summary="Get stats for a specific fetcher")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public FetcherGlobalStats fetcherStats ( @PathVariable @NotBlank final String crawlerNodeName) {
		return fetcherOrchestrationService.getCrawlerStatuses().asMap().get(crawlerNodeName);
	}


	@GetMapping(path=UrlConstants.MASTER_API_CRAWLERS  + UrlConstants.CRAWLER_API_STOP_FETCHING)
	@Operation(summary="Stop a fetching job, will request any fetchers")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void stopFetching(  @RequestParam @NotBlank final String provider) throws InvalidParameterException{
		// Get the providerName corresponding to the url
		final DataSourceProperties dsp = datasourceConfigService.getDatasourceConfig(provider);
		if (null == dsp) {
			throw new InvalidParameterException("Cannot find a matching DatasourceProperties for " + provider);
		}

		fetcherOrchestrationService.stop(dsp, dsp);
	}

	
	@PutMapping(path=UrlConstants.MASTER_API_CRAWLER_UPDATE_PREFIX+"{crawlerNodeName}",consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary="Update the presence and status of a Fetcher")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_CRAWLER+"')")
	@Hidden
	public void updateFetcherStatus( @PathVariable @NotBlank final String crawlerNodeName, @RequestBody @NotBlank final FetcherGlobalStats globalStats) {
		fetcherOrchestrationService.updateClientStatus(globalStats);
	}

	@PostMapping(path=UrlConstants.MASTER_API_CRAWLER_UPDATE_PREFIX+"{crawlerNodeName}"+"/"+"{datasourceName}"  + UrlConstants.MASTER_API_CRAWLER_TRIGGER_SUFFIX)
	@Operation(summary="Run a datasource retrieving against a specific node")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	@Hidden
	public FetchRequestResponse triggerFetcher( @PathVariable @NotBlank final String crawlerNodeName, @PathVariable @NotBlank final String datasourceName) {
		return fetcherOrchestrationService.triggerRemoteCrawling(crawlerNodeName, datasourceName);
	}

}
