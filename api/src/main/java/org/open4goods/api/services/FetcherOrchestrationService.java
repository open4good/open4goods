package org.open4goods.api.services;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.model.constants.TimeConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.crawlers.FetcherGlobalStats;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.dto.FetchRequestResponse;
import org.open4goods.services.DataSourceConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


/**
 * This service is in charge of fetcher jobs orchestration.
 * @author Goulven.Furet
 *
 *
 */
public class FetcherOrchestrationService {

	protected static final Logger logger = LoggerFactory.getLogger(FetcherOrchestrationService.class);

	private final  ThreadPoolTaskScheduler threadPoolTaskScheduler;

	private final  DataSourceConfigService datasourceConfigService;

	// The status of the registered crawlers, which auto expires the crawlers if not seen
	private Cache<String, FetcherGlobalStats> crawlerStatuses = CacheBuilder.newBuilder()
		    .expireAfterWrite(TimeConstants.API_EXPIRED_UNSEEN_CRAWLERS_IN_SECONDS, TimeUnit.SECONDS)
		    .build( );

	public FetcherOrchestrationService(final ThreadPoolTaskScheduler threadPoolTaskScheduler,
			final DataSourceConfigService datasourceConfigService) {
		super();
		this.threadPoolTaskScheduler = threadPoolTaskScheduler;
		this.datasourceConfigService = datasourceConfigService;
	}


	/**
	 * Schedule each datasource fetching, based on the cron in the DataSource configurations
	 */
	@PostConstruct
	public void schedule() {
		logger.info("Initialising direct datasources scheduling");

		final Map<String,DataSourceProperties> providerConfigs = datasourceConfigService.getDatasourceConfigs();
		for (final Entry<String, DataSourceProperties> pConf : providerConfigs.entrySet()) {

			final String realCron = pConf.getValue().cron();

			final CronTrigger t = new CronTrigger(realCron);

			threadPoolTaskScheduler.schedule(() -> {
				logger.warn("triggerRemoteCrawling crawl of {}", pConf.getKey());
				triggerRemoteCrawling(pConf.getValue(), pConf.getKey());
			}, t);
		}
	}

	/**
	 * Update the internally maintained map of crawlers with the one given in parameter
	 * @param clientStats
	 */
	public void updateClientStatus(final FetcherGlobalStats clientStats) {
		logger.debug("Got a status message from {}",clientStats.getNodeConfig().getNodeName() );
		crawlerStatuses.put(clientStats.getNodeConfig().getNodeName(), clientStats);

	}


	/**
	 * Trigger a crawl for the given provider
	 * @param providerName
	 * @return
	 */
	public FetchRequestResponse triggerRemoteCrawling( final String datasourceConfName) {

		// Checking if there is not a running fetching job for this provider

		for (final Entry<String, DataSourceProperties> p : datasourceConfigService.getDatasourceConfigs().entrySet()) {
			if (p.getKey().equals(datasourceConfName)) {
				return triggerRemoteCrawling(p.getValue(), datasourceConfName);
			}
		}

		final FetchRequestResponse response = new FetchRequestResponse(false, "Fetcher " + datasourceConfName + " was not found");
		return response;

	}


	/**
	 * Trigger a crawl for the given provider
	 * @param providerName
	 * @return
	 */
	public FetchRequestResponse triggerRemoteCrawling(final String nodeName, final String datasourceConfName) {

		// Checking if there is not a running fetching job for this provider

		for (final Entry<String, DataSourceProperties> p : datasourceConfigService.getDatasourceConfigs().entrySet()) {
			if (p.getKey().equals(datasourceConfName)) {
				return triggerRemoteCrawling(nodeName, p.getValue(),datasourceConfName);
			}
		}

		final FetchRequestResponse response = new FetchRequestResponse(false, "Fetcher " + datasourceConfName + " was not found");
		return response;

	}

	public FetchRequestResponse triggerRemoteCrawling(@NotBlank final String nodeName, @NotBlank final DataSourceProperties p, final String datasourceConfName) {

		final FetcherGlobalStats node = crawlerStatuses.getIfPresent(nodeName);

		if (null == node) {
			return new FetchRequestResponse(false, "Node " + nodeName + " is unknown");
		}
		else {
			return triggerRemoteFetching(node, p,datasourceConfName);
		}

	}


	public void stop(final DataSourceProperties dsp, final DataSourceProperties p) {
		// Sends the stop command against all crawlers

		final Collection<FetcherGlobalStats> fetchers = crawlerStatuses.asMap().values();

		for (final FetcherGlobalStats f : fetchers)  {
			stopRemoteFetching(f, p);
		}

	}

	public FetchRequestResponse stopRemoteFetching(@NotBlank final FetcherGlobalStats node, @NotBlank final DataSourceProperties p) {
		////////////////////
		// Trigger remote crawl
		////////////////

		// Calling the remote API
		final RestTemplate restTemplate = new RestTemplate();

		try {
			final RequestEntity<DataSourceProperties> requestEntity = RequestEntity
								.post(new URL(node.getNodeConfig().getNodeUrl() + UrlConstants.CRAWLER_API_STOP_FETCHING).toURI())
								.contentType(MediaType.APPLICATION_JSON)
								.body(p);

			final ResponseEntity<FetchRequestResponse> ret = restTemplate.exchange(requestEntity, FetchRequestResponse.class);

			if (ret.getBody().isCrawlAccepted()) {
				logger.info("Fetch stopping of {} has been accepted by : {} ",p.getName(), node.getNodeConfig().getNodeName());

			} else {
				logger.warn("Fetch stopping of {} has been discarded by : {}. Reason is : {} ", p.getName(), node.getNodeConfig().getNodeName(), ret.getBody().getMessage());
			}
			return ret.getBody();

		} catch (final Exception e) {
			logger.error("Unexpected error while in Fetch request to : {}. Reason is : {} ", node.getNodeConfig().getNodeUrl(), e);
			return new FetchRequestResponse(false,"Unexpected error : " + e.getMessage() );
		}
	}



	public FetchRequestResponse triggerRemoteFetching(@NotBlank final FetcherGlobalStats node, @NotBlank final DataSourceProperties p, final String datasourceConfName) {
		////////////////////
		// Trigger remote crawl
		////////////////

		// Calling the remote API
		final RestTemplate restTemplate = new RestTemplate();

		try {
			final RequestEntity<DataSourceProperties> requestEntity = RequestEntity
								.post(new URL(node.getNodeConfig().getNodeUrl() + UrlConstants.CRAWLER_API_REQUEST_FETCHING+"?datasourceConfName="+datasourceConfName).toURI())

								.contentType(MediaType.APPLICATION_JSON)
								.body(p)

								;

			final ResponseEntity<FetchRequestResponse> ret = restTemplate.exchange(requestEntity, FetchRequestResponse.class);

			if (ret.getBody().isCrawlAccepted()) {
				logger.info("Fetch request of {} has been accepted by : {} ",datasourceConfName, node.getNodeConfig().getNodeName());

			} else {
				logger.warn("Fetch request of {} has been discarded by : {}. Reason is : {} ", datasourceConfName, node.getNodeConfig().getNodeName(), ret.getBody().getMessage());
			}
			return ret.getBody();

		} catch (final Exception e) {
			logger.error("Unexpected error while in Fetch request to : {}. Reason is : {} ", node.getNodeConfig().getNodeUrl(), e);
			return new FetchRequestResponse(false,"Unexpected error : " + e.getMessage() );
		}
	}

	/**
	 * Trigger a fetch for the given provider against the less busy node
	 * @param p
	 * @return
	 */
	public FetchRequestResponse triggerRemoteCrawling(final DataSourceProperties p, final String datasourceConfName) {

		if (isRunning(datasourceConfName)) {
			return new FetchRequestResponse(false,"There is already a running job for " + p.getName());
		}

		//////////////////////////
		//  Elect the less busy crawler
		//////////////////////////
		final FetcherGlobalStats electedNode = getLessBusyNode();
		return triggerRemoteFetching(electedNode, p,datasourceConfName);

	}





	/**
	 * Trigger a synchronous url fetching
	 * @param p
	 * @return
	 */
	public DataFragment triggerHttpSynchFetching(final DataSourceProperties p, final String url) {

		//////////////
		//  Elect the less busy crawler
		//////////////
		final FetcherGlobalStats electedNode = getLessBusyNode();

		////////////////////
		// Trigger remote crawl
		////////////////

		final RestTemplate restTemplate = new RestTemplate();

		try {
			final RequestEntity<DataSourceProperties> requestEntity = RequestEntity
								.post(new URL(electedNode.getNodeConfig().getNodeUrl() + UrlConstants.CRAWLER_API_DIRECT_URL_REQUEST_FETCHING  +"?"+UrlConstants.URL_PARAMETER + "=" + URLEncoder.encode( url)).toURI())
								.contentType(MediaType.APPLICATION_JSON)
								.body(p);

			final ResponseEntity<DataFragment> ret = restTemplate.exchange(requestEntity, DataFragment.class);
			return ret.getBody();
		} catch (final Exception e) {
			logger.error("Unexpected error while in synchronous http fetch request to : {}. Reason is : {} ", electedNode.getNodeConfig().getNodeUrl(), e.getMessage());

		}
		return null;
	}


//	/**
//	 * Trigger a synchronous csv line fetching
//	 * @param p
//	 * @return
//	 */
//	public DataFragment triggerCsvSynchFetching(final DataSourceProperties p, final String csvLine, final String csvHeaders) {
//
//		//////////////
//		//  Elect the less busy crawler
//		//////////////
//		final FetcherGlobalStats electedNode = getLessBusyNode();
//
//		////////////////////
//		// Trigger remote crawl
//		////////////////
//
//		final RestTemplate restTemplate = new RestTemplate();
//
//		try {
//			final RequestEntity<DataSourceProperties> requestEntity = RequestEntity
//								.post(new URL(electedNode.getNodeConfig().getNodeUrl() + UrlConstants.CRAWLER_API_DIRECT_CSV_REQUEST_FETCHING  +"?"+UrlConstants.CSV_LINE_PARAMETER + "=" + URLEncoder.encode( csvLine)  +"&"+ UrlConstants.CSV_HEADERS_PARAMETER+"="+URLEncoder.encode( csvHeaders)).toURI())
//								.contentType(MediaType.APPLICATION_JSON)
//								.body(p);
//
//			final ResponseEntity<DataFragment> ret = restTemplate.exchange(requestEntity, DataFragment.class);
//			return ret.getBody();
//		} catch (final Exception e) {
//			logger.error("Unexpected error while in synchronous csv fetch request to : {}. Reason is : {} ", electedNode.getNodeConfig().getNodeUrl(), e.getMessage());
//
//		}
//		return null;
//	}


	/**
	 *
	 * @param datasourceName
	 * @return true if a fetching job exists for this datasource
	 */
	public boolean isRunning(final String datasourceName) {
		return crawlerStatuses.asMap().values().stream().anyMatch(e -> e.containsDatasource(datasourceName));
	}

	/**
	 * Return the less busy fetcher node
	 * @return
	 */
	private FetcherGlobalStats getLessBusyNode() {
		FetcherGlobalStats electedNode = null;

		for (final FetcherGlobalStats gs : crawlerStatuses.asMap().values()) {

			if (null == electedNode) {
				electedNode = gs;
			} else {
				// Election based on the number of running crawlers
				if (gs.getCrawlerStats().size() < electedNode.getCrawlerStats().size() ) {
					electedNode = gs;
				}
			}
		}
		return electedNode;
	}


	public Cache<String, FetcherGlobalStats> getCrawlerStatuses() {
		return crawlerStatuses;
	}

	public void setCrawlerStatuses(final Cache<String, FetcherGlobalStats> crawlerStatuses) {
		this.crawlerStatuses = crawlerStatuses;
	}





}
