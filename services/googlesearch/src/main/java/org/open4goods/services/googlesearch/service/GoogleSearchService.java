package org.open4goods.services.googlesearch.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.open4goods.services.googlesearch.config.GoogleSearchProperties;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with the Google Custom Search API.
 * <p>
 * This service externalizes its configuration, increments an actuator metric for each search,
 * and implements a health check by verifying that the necessary properties are set.
 */
@Service
public class GoogleSearchService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSearchService.class);

    private final HttpClient httpClient;
    private final Gson gson;
    private final GoogleSearchProperties properties;
    private final MeterRegistry meterRegistry;
    
    // State variable to hold the last error message if a non-200 response is encountered.
    private volatile String lastErrorMessage = null;

    /**
     * Constructs a new GoogleSearchService.
     *
     * @param properties  the Google search configuration properties
     * @param meterRegistry the actuator meter registry for metrics
     */
    public GoogleSearchService(GoogleSearchProperties properties, MeterRegistry meterRegistry) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Executes a search against the Google Custom Search API.
     *
     * @param request a {@link GoogleSearchRequest} containing the query and desired number of results
     * @return a {@link GoogleSearchResponse} containing the search results
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public GoogleSearchResponse search(GoogleSearchRequest request) throws IOException, InterruptedException {
        // Increment the actuator metric for the number of searches
        meterRegistry.counter("google.search.count").increment();

        // Build the URL dynamically using externalized configuration and URL-encoded query
        String url = String.format("%s?q=%s&key=%s&cx=%s&num=%d",
                properties.getSearchUrl(),
                URLEncoder.encode(request.getQuery(), Charset.defaultCharset()),
                properties.getApiKey(),
                properties.getCx(),
                request.getNumResults());

        logger.debug("Executing search with URL: {}", url);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        // Send the HTTP request and get the response
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Check the HTTP status code and raise exception if not successful
        if (response.statusCode() != 200) {
            String errorMsg = "Error from Google Custom Search API: HTTP " + response.statusCode() + " - " + response.body();
            logger.error(errorMsg);
            lastErrorMessage = errorMsg;
            throw new RuntimeException(errorMsg);
        } else {
            // Clear any previous error if the current call is successful.
            lastErrorMessage = null;
        }

        logger.info("Search performed for query: '{}' with HTTP status: {}", request.getQuery(), response.statusCode());

        return parseResponse(response.body());
    }

    /**
     * Parses the JSON response returned by the Google Custom Search API.
     *
     * @param jsonResponse the raw JSON response as a String
     * @return a {@link GoogleSearchResponse} containing the parsed search results
     */
    private GoogleSearchResponse parseResponse(String jsonResponse) {
        logger.debug("Parsing response JSON: {}", jsonResponse);
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
        JsonArray items = jsonObject.getAsJsonArray("items");

        List<GoogleSearchResult> results = new ArrayList<>();
        if (items != null) {
            // Iterate over each item and extract title and link
            items.forEach(item -> {
                JsonObject obj = item.getAsJsonObject();
                String title = obj.get("title").getAsString();
                String link = obj.get("link").getAsString();
                results.add(new GoogleSearchResult(title, link));
            });
        }

        return new GoogleSearchResponse(results);
    }

    /**
     * Health check implementation.
     * <p>
     * The service is considered healthy if the necessary configuration properties are set and the last search returned HTTP 200.
     *
     * @return a Health status indicating UP if properties are properly configured and no recent error was encountered, otherwise DOWN
     */
    @Override
    public Health health() {
        if (properties.getApiKey() == null || properties.getApiKey().isEmpty() ||
            properties.getCx() == null || properties.getCx().isEmpty() ||
            properties.getSearchUrl() == null || properties.getSearchUrl().isEmpty()) {
            logger.error("Google Search properties are not properly configured.");
            return Health.down().withDetail("error", "Google Search properties are missing or invalid").build();
        }
        
        if (lastErrorMessage != null) {
            logger.error("Health check failed due to previous error: {}", lastErrorMessage);
            return Health.down().withDetail("error", lastErrorMessage).build();
        }
        
        logger.debug("Google Search properties are properly configured and no recent errors encountered.");
        return Health.up().build();
    }
}
