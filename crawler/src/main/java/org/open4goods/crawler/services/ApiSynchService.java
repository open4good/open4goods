package org.open4goods.crawler.services;

import java.util.List;

import org.open4goods.commons.model.constants.TimeConstants;
import org.open4goods.commons.model.crawlers.ApiSynchConfig;
import org.open4goods.commons.model.crawlers.FetcherGlobalStats;
import org.open4goods.model.constants.UrlConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Periodically reports crawler node status to the master API.
 */
public class ApiSynchService {

    private static final Logger logger = LoggerFactory.getLogger(ApiSynchService.class);

    private final ApiSynchConfig apiSynchConfig;
    private final FetchersService fetchersService;
    private final String masterEndPoint;
    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public ApiSynchService(final ApiSynchConfig apiSynchConfig, final FetchersService fetchersService, final String masterEndPoint, final String apiKey) {
        this.apiSynchConfig = apiSynchConfig;
        this.fetchersService = fetchersService;
        this.masterEndPoint = masterEndPoint;
        this.apiKey = apiKey;
    }

    /** Sends node status to the master API on a fixed schedule. */
    @Scheduled(initialDelay = 0L, fixedDelay = TimeConstants.CRAWLER_UPDATE_STATUS_TO_API_MS)
    public void updateStatus() {
        logger.debug("Updating status of {} against master API", apiSynchConfig.getNodeName());

        final FetcherGlobalStats statusObject = fetchersService.stats();
        final String url = masterEndPoint + String.format(
                UrlConstants.MASTER_API_CRAWLER_UPDATE + "?" + UrlConstants.APIKEY_PARAMETER + "=" + apiKey,
                apiSynchConfig.getNodeName());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            var response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(statusObject, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Update status of {} against master API is OK", apiSynchConfig.getNodeName());
            } else {
                logger.warn("Update status of {} against master API is KO. Status code is {}", apiSynchConfig.getNodeName(), response.getStatusCode());
            }
        } catch (final RestClientException e) {
            logger.error("Error while updating crawl status of {} to master: {}", apiSynchConfig.getNodeName(), e.getMessage());
        }
    }
}
