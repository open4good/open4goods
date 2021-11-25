package org.open4goods.crawler.services;

import org.open4goods.model.constants.TimeConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.crawlers.ApiSynchConfig;
import org.open4goods.model.crawlers.FetcherGlobalStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * This service is in charge of declaring the crawler instance on the master API, then it will periodically send status informations
 * @author goulven
 *
 */
public class ApiSynchService {

	private static final Logger logger = LoggerFactory.getLogger(ApiSynchService.class);

	private final ApiSynchConfig apiSynchConfig;

	private final FetchersService fetchersService;

	private final String masterEndPoint;

	private final String apiKey;

	public ApiSynchService(final ApiSynchConfig apiSynchConfig, final FetchersService fetchersService, final String masterEndPoint, final String apiKey) {
		super();
		this.apiSynchConfig = apiSynchConfig;
		this.fetchersService = fetchersService;
		this.masterEndPoint = masterEndPoint;
		this.apiKey = apiKey;
	}


	/**
	 * Declare
	 */
	@Scheduled(initialDelay = 0L, fixedDelay=TimeConstants.CRAWLER_UPDATE_STATUS_TO_API_MS )
	public void updateStatus () {
		logger.debug	("Updating status of {} against master API",apiSynchConfig.getNodeName());

		// Getting the fetchersService stats

		final FetcherGlobalStats statusObject = fetchersService.stats();
		// Calling the remote API
				try {
					final HttpResponse<JsonNode> jsonResponse = Unirest.put(masterEndPoint + String.format(UrlConstants.MASTER_API_CRAWLER_UPDATE+"?"+UrlConstants.APIKEY_PARAMETER+"="+apiKey,apiSynchConfig.getNodeName()) )
							.header("accept", MediaType.APPLICATION_JSON_VALUE)
							.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//							.header("accept", "application/json")
							.body(statusObject)
							.asJson();

					if (200 == jsonResponse.getStatus() ) {
						logger.debug("Update status of {} against master API is OK",apiSynchConfig.getNodeName());
					} else {
						logger.warn("Update status of {} against master API is KO. Satus code is {}",apiSynchConfig.getNodeName(),jsonResponse.getStatus() );
					}
				}
				catch (final UnirestException e) {
					logger.error("Error while updating crawl status of {} to master : {} ", apiSynchConfig.getNodeName(), e.getMessage());
				}
	}
}
