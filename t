* Directory structure *
- pom.xml
- README.md
- src
--- main
---- java
----- org
------ open4goods
------- services
-------- googlesearch
--------- config
---------- GoogleSearchConfig.java
--------- dto
---------- GoogleSearchRequest.java
---------- GoogleSearchResponse.java
---------- GoogleSearchResult.java
--------- exception
---------- GoogleSearchException.java
--------- service
---------- GoogleSearchService.java
---- resources
----- META-INF
------ additional-spring-configuration-metadata.json
--- test
---- java
----- org
------ open4goods
------- googlesearch
-------- GoogleSearchServiceTest.java
---- resources
----- application-test.yml

* Files content *

** [services/googlesearch//pom.xml] **
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  
  <parent>
    <groupId>org.open4goods</groupId>
    <artifactId>org.open4goods</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </parent>
  
  <artifactId>googlesearch</artifactId>
  
  
  <dependencies>
    <!-- Spring Boot Actuator for health and metrics -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Gson for JSON processing -->
    <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
    </dependency>

    <!-- Configuration Processor for metadata generation -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-configuration-processor</artifactId>
      <optional>true</optional>
    </dependency>
    
    <!-- Test Dependencies -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  
  <build>
    <plugins>
      <!-- Maven Compiler Plugin -->
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.14.0</version>
        <configuration>
          <source>${java.version}</source>
          <target>${java.version}</target>
        </configuration>
      </plugin>
      <!-- Spring Boot Maven Plugin -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>

** [services/googlesearch//README.md] **
# GoogleSearch Service

This service is part of the [open4goods](https://github.com/open4good/open4goods) project. It provides a Spring Boot-based integration with the Google Custom Search API.

## Overview

The **GoogleSearch Service** allows you to perform searches against the Google Custom Search API using externalized configuration. It also integrates with Spring Boot Actuator for health checks and metrics.

## Features

- **Custom Search API Integration:** Execute searches using a configurable API key, search engine ID (cx), and search URL.
- **Health Check:** The service implements a health indicator that checks if the necessary properties are configured and if the last search call was successful.
- **Metrics:** Each search increments an actuator metric (`google.search.count`).

## Configuration

Configuration properties can be set in your `application.yml` or `application-test.yml`. For example:

```yaml
googlesearch:
  apiKey: "YOUR_API_KEY"
  cx: "YOUR_CX"
  searchUrl: "https://www.googleapis.com/customsearch/v1"
```

## How to Use

1. **Include the Dependency:**

   Make sure to include the `googlesearch` module as a dependency in your project (it is built as a JAR).

2. **Autowire the Service:**

   In your Spring Boot application, autowire the `GoogleSearchService`:
   
   ```java
   @Autowired
   private GoogleSearchService googleSearchService;
   ```

3. **Perform a Search:**

   Create a search request and execute the search:
   
   ```java
   GoogleSearchRequest request = new GoogleSearchRequest("Spring Boot", 5);
   try {
       GoogleSearchResponse response = googleSearchService.search(request);
       response.getResults().forEach(result ->
           System.out.println("Title: " + result.getTitle() + ", Link: " + result.getLink())
       );
   } catch (Exception e) {
       // Handle error
       e.printStackTrace();
   }
   ```

## Testing

A sample unit test is provided in `src/test/java/org/open4goods/googlesearch/GoogleSearchServiceTest.java`. The test configuration is bootstrapped using an `application-test.yml`.

## Maven Build

The Maven POM is configured with the necessary dependencies and plugins. To build the service, run:

```bash
mvn clean install
```

## License

This project is licensed under the terms of the [MIT License](LICENSE).

** [services/googlesearch//src/main/resources/META-INF/additional-spring-configuration-metadata.json] **
{
  "groups": [
    {
      "name": "googlesearch",
      "type": "org.open4goods.services.googlesearch.config.GoogleSearchProperties",
      "sourceType": "org.open4goods.services.googlesearch.config.GoogleSearchProperties"
    }
  ],
  "properties": [
    {
      "name": "googlesearch.apiKey",
      "type": "java.lang.String",
      "description": "API key for accessing the Google Custom Search API."
    },
    {
      "name": "googlesearch.cx",
      "type": "java.lang.String",
      "description": "Search engine identifier (cx) for the Google Custom Search API."
    },
    {
      "name": "googlesearch.searchUrl",
      "type": "java.lang.String",
      "description": "The URL endpoint for the Google Custom Search API."
    }
  ]
}

** [services/googlesearch//src/main/java/org/open4goods/services/googlesearch/service/GoogleSearchService.java] **
package org.open4goods.services.googlesearch.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.open4goods.services.googlesearch.config.GoogleSearchConfig;

import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.exception.GoogleSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service for interacting with the Google Custom Search API.
 * <p>
 * It externalizes its configuration via {@link GoogleSearchProperties}, increments actuator metrics for each search,
 * and implements a health check based on proper configuration and recent API call outcomes.
 */
@Service
public class GoogleSearchService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSearchService.class);

    private final HttpClient httpClient;
    private final Gson gson;
    private final GoogleSearchConfig properties;
    private final MeterRegistry meterRegistry;
    
    // Volatile variable to hold the last error message if a non-200 response was received.
    private volatile String lastErrorMessage = null;

    /**
     * Constructs a new GoogleSearchService.
     *
     * @param properties    the Google search configuration properties
     * @param meterRegistry the actuator meter registry for metrics
     */
    public GoogleSearchService(GoogleSearchConfig properties, MeterRegistry meterRegistry) {
        // Create an HttpClient with a connection timeout.
        this.httpClient = HttpClient.newBuilder()
                                    .connectTimeout(Duration.ofSeconds(10))
                                    .build();
        this.gson = new Gson();
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Executes a search against the Google Custom Search API.
     *
     * @param request a {@link GoogleSearchRequest} containing the query and desired number of results
     * @return a {@link GoogleSearchResponse} containing the search results
     * @throws IOException          if an I/O error occurs during the HTTP call
     * @throws InterruptedException if the HTTP request is interrupted
     * @throws GoogleSearchException if the API responds with an error or the response cannot be parsed
     */
    public GoogleSearchResponse search(GoogleSearchRequest request) throws IOException, InterruptedException {
        // Increment the actuator metric for the number of searches performed.
        meterRegistry.counter("google.search.count").increment();

        // Validate input query (constructor of GoogleSearchRequest already does basic validation)
        final String encodedQuery = URLEncoder.encode(request.getQuery(), Charset.defaultCharset());
        
        // Build the API URL using externalized configuration.
        final String url = String.format("%s?q=%s&key=%s&cx=%s&num=%d",
                properties.getSearchUrl(),
                encodedQuery,
                properties.getApiKey(),
                properties.getCx(),
                request.getNumResults());
        
        // Prepare a safe version of the URL for logging (masking the API key)
        final String safeUrl = String.format("%s?q=%s&key=****&cx=%s&num=%d",
                properties.getSearchUrl(),
                encodedQuery,
                properties.getCx(),
                request.getNumResults());
        logger.debug("Executing search with URL: {}", safeUrl);

        // Build the HTTP request with a timeout.
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        // Send the HTTP request and obtain the response.
        final HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Check the HTTP status code; if not 200, log and throw a custom exception.
        if (response.statusCode() != 200) {
            String errorMsg = "Error from Google Custom Search API: HTTP " + response.statusCode() + " - " + response.body();
            logger.error(errorMsg);
            lastErrorMessage = errorMsg;
            throw new GoogleSearchException(errorMsg);
        } else {
            // Clear any previous error if the current call is successful.
            lastErrorMessage = null;
        }

        logger.info("Search performed for query: '{}' with HTTP status: {}", request.getQuery(), response.statusCode());

        // Parse the JSON response into our DTO.
        return parseResponse(response.body());
    }

    /**
     * Parses the JSON response returned by the Google Custom Search API.
     *
     * @param jsonResponse the raw JSON response as a String
     * @return a {@link GoogleSearchResponse} containing the parsed search results
     * @throws GoogleSearchException if parsing fails or the expected JSON structure is missing
     */
    private GoogleSearchResponse parseResponse(String jsonResponse) {
        logger.debug("Parsing response JSON: {}", jsonResponse);
        try {
            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray items = jsonObject.getAsJsonArray("items");

            final List<GoogleSearchResult> results = new ArrayList<>();
            if (items != null) {
                // Iterate over each item and extract title and link.
                items.forEach(item -> {
                    JsonObject obj = item.getAsJsonObject();
                    String title = obj.get("title").getAsString();
                    String link = obj.get("link").getAsString();
                    results.add(new GoogleSearchResult(title, link));
                });
            }
            return new GoogleSearchResponse(results);
        } catch (Exception e) {
            String errorMsg = "Failed to parse Google Custom Search API response";
            logger.error(errorMsg, e);
            throw new GoogleSearchException(errorMsg, e);
        }
    }

    /**
     * Health check implementation.
     * <p>
     * The service is considered healthy if the necessary configuration properties are set and the last search returned HTTP 200.
     *
     * @return a Health status indicating UP if properties are properly configured and no recent error was encountered; otherwise DOWN.
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

** [services/googlesearch//src/main/java/org/open4goods/services/googlesearch/dto/GoogleSearchRequest.java] **
package org.open4goods.services.googlesearch.dto;

import java.util.Objects;

/**
 * Data Transfer Object representing a Google Search Request.
 */
public class GoogleSearchRequest {
    
    private final String query;
    private final int numResults;

    /**
     * Constructs a new GoogleSearchRequest.
     *
     * @param query      the search query (must not be null or empty)
     * @param numResults the number of results to retrieve
     */
    public GoogleSearchRequest(String query, int numResults) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be null or empty");
        }
        this.query = query;
        this.numResults = numResults;
    }

    public String getQuery() {
        return query;
    }

    public int getNumResults() {
        return numResults;
    }

    @Override
    public String toString() {
        return "GoogleSearchRequest{" +
                "query='" + query + '\'' +
                ", numResults=" + numResults +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, numResults);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchRequest)) return false;
        GoogleSearchRequest that = (GoogleSearchRequest) o;
        return numResults == that.numResults &&
               Objects.equals(query, that.query);
    }
}

** [services/googlesearch//src/main/java/org/open4goods/services/googlesearch/dto/GoogleSearchResult.java] **
package org.open4goods.services.googlesearch.dto;

import java.util.Objects;

/**
 * Data Transfer Object representing an individual search result.
 */
public class GoogleSearchResult {

    private final String title;
    private final String link;

    /**
     * Constructs a new GoogleSearchResult.
     *
     * @param title the title of the search result
     * @param link  the URL of the search result
     */
    public GoogleSearchResult(String title, String link) {
        this.title = title;
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    @Override
    public String toString() {
        return "GoogleSearchResult{" +
                "title='" + title + '\'' +
                ", link='" + link + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, link);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchResult)) return false;
        GoogleSearchResult that = (GoogleSearchResult) o;
        return Objects.equals(title, that.title) &&
               Objects.equals(link, that.link);
    }
}

** [services/googlesearch//src/main/java/org/open4goods/services/googlesearch/dto/GoogleSearchResponse.java] **
package org.open4goods.services.googlesearch.dto;

import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object representing a Google Search Response.
 */
public class GoogleSearchResponse {

    private final List<GoogleSearchResult> results;

    /**
     * Constructs a new GoogleSearchResponse.
     *
     * @param results the list of search results
     */
    public GoogleSearchResponse(List<GoogleSearchResult> results) {
        this.results = results;
    }

    public List<GoogleSearchResult> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "GoogleSearchResponse{" +
                "results=" + results +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(results);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchResponse)) return false;
        GoogleSearchResponse that = (GoogleSearchResponse) o;
        return Objects.equals(results, that.results);
    }
}

** [services/googlesearch//src/main/java/org/open4goods/services/googlesearch/config/GoogleSearchConfig.java] **
package org.open4goods.services.googlesearch.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Google Custom Search.
 * <p>
 * These properties are loaded from the application.yml (or application-test.yml for tests).
 * Example configuration:
 * <pre>
 * googlesearch:
 *   apiKey: "YOUR_API_KEY"
 *   cx: "YOUR_CX"
 *   searchUrl: "https://www.googleapis.com/customsearch/v1"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "googlesearch")
public class GoogleSearchConfig {

    private String apiKey;
    private String cx;
    private String searchUrl = "https://www.googleapis.com/customsearch/v1";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCx() {
        return cx;
    }

    public void setCx(String cx) {
        this.cx = cx;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    @Override
    public String toString() {
        return "GoogleSearchConfig{" +
                "apiKey='****'" + // Do not expose the API key in logs
                ", cx='" + cx + '\'' +
                ", searchUrl='" + searchUrl + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, cx, searchUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchConfig)) return false;
        GoogleSearchConfig that = (GoogleSearchConfig) o;
        return Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(cx, that.cx) &&
               Objects.equals(searchUrl, that.searchUrl);
    }
}

** [services/googlesearch//src/main/java/org/open4goods/services/googlesearch/exception/GoogleSearchException.java] **
package org.open4goods.services.googlesearch.exception;

/**
 * Exception thrown when an error occurs while interacting with the Google Custom Search API.
 */
public class GoogleSearchException extends RuntimeException {

    /**
     * Constructs a new GoogleSearchException with the specified detail message.
     *
     * @param message the detail message
     */
    public GoogleSearchException(String message) {
        super(message);
    }

    /**
     * Constructs a new GoogleSearchException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public GoogleSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}

** [services/googlesearch//src/test/resources/application-test.yml] **
googlesearch:
  apiKey: "API-KEY"
  cx: "API-CX"
  searchUrl: "https://www.googleapis.com/customsearch/v1"

** [services/googlesearch//src/test/java/org/open4goods/googlesearch/GoogleSearchServiceTest.java] **
package org.open4goods.googlesearch;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for {@link GoogleSearchService}.
 */
@SpringBootTest
@ActiveProfiles("test")
public class GoogleSearchServiceTest {

    @Autowired
    private GoogleSearchService googleSearchService;

    /**
     * Test that the search method returns an error for a bad request.
     * <p>
     * This test expects the search to fail with an error message containing "400".
     */
    @Test
    public void testSearch() throws IOException, InterruptedException {
        // Prepare a sample search request with a known query.
        GoogleSearchRequest request = new GoogleSearchRequest("Spring Boot", 5);
        
        try {
            googleSearchService.search(request);
        } catch (Exception e) {
            // We expect an error response due to test configuration (e.g., invalid API key)
            assertTrue(e.getMessage().contains("400") || e.getMessage().contains("Error"));
            return;
        }
        fail("Search should have failed with a 400 exception");
    }

    /**
     * Test that the health check returns UP when properties are properly configured.
     */
    @Test
    public void testHealth() {
        // Verify that the health indicator returns a status of UP
        assertEquals(Status.UP, googleSearchService.health().getStatus(), "Health check should be UP");
    }

    /**
     * Minimal test configuration to bootstrap the Spring context.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"org.open4goods.services.googlesearch"})
    public static class TestConfig {
        // This class remains empty; its purpose is to trigger component scanning in the
        // org.open4goods.services.googlesearch package and enable auto-configuration.
    }
}

