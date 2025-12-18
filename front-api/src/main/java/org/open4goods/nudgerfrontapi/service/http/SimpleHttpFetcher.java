package org.open4goods.nudgerfrontapi.service.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Lightweight HTTP fetcher reused by share resolution to hit crawler endpoints
 * or remote URLs.
 */
@Service
public class SimpleHttpFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHttpFetcher.class);

    private final RestClient restClient;

    public SimpleHttpFetcher(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Fetch the raw body of the provided URL using a simple GET request.
     *
     * @param url target URL
     * @return body when the request succeeds or {@code null} otherwise
     */
    public String fetch(String url) {
        try {
            return restClient.get().uri(url).retrieve().body(String.class);
        } catch (RestClientResponseException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            LOGGER.warn("HTTP {} while fetching {} for share resolution: {}", statusCode, url, e.getResponseBodyAsString());
            return null;
        } catch (RestClientException e) {
            LOGGER.warn("HTTP client error while fetching {}: {}", url, e.getMessage());
            return null;
        }
    }
}
