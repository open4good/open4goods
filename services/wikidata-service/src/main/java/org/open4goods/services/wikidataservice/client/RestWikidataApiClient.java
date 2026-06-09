package org.open4goods.services.wikidataservice.client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.open4goods.services.wikidataservice.config.WikidataServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

/**
 * Spring {@link RestClient}-based implementation of {@link WikidataApiClient}.
 *
 * <p>Respects the Wikidata data-access policy:
 * <ul>
 *   <li>Sends an identifiable {@code User-Agent} on every request.</li>
 *   <li>Handles HTTP 429 by honouring the {@code Retry-After} header before retrying once.</li>
 * </ul>
 *
 * @see <a href="https://www.wikidata.org/wiki/Wikidata:Data_access">Wikidata data access policy</a>
 */
public class RestWikidataApiClient implements WikidataApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestWikidataApiClient.class);
    private static final int MAX_RETRY_AFTER_SECONDS = 60;

    private final RestClient restClient;
    private final WikidataServiceProperties properties;
    private final ObjectMapper objectMapper;

    public RestWikidataApiClient(WikidataServiceProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEntity(String qId, List<String> languages) {
        if (qId == null || qId.isBlank()) {
            return Collections.emptyMap();
        }
        String langs = String.join("|", languages);
        String url = properties.getRestApiBase()
                + "?action=wbgetentities"
                + "&ids=" + qId
                + "&languages=" + langs
                + "&props=labels|aliases|descriptions|claims|sitelinks"
                + "&sitefilter=" + buildSitefilter(languages)
                + "&format=json";

        try {
            String body = fetchWithRetry(url);
            if (body == null) {
                return Collections.emptyMap();
            }
            Map<String, Object> root = objectMapper.readValue(body, Map.class);
            Map<String, Object> entities = (Map<String, Object>) root.get("entities");
            if (entities == null || !entities.containsKey(qId)) {
                return Collections.emptyMap();
            }
            Object entity = entities.get(qId);
            if (entity instanceof Map<?, ?> entityMap) {
                return (Map<String, Object>) entityMap;
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Error fetching Wikidata entity {}", qId, e);
            } else {
                LOGGER.warn("Error fetching Wikidata entity {}: {}", qId, e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEntityClaims(String qId, List<String> languages) {
        Map<String, Object> entity = getEntity(qId, languages);
        if (entity.isEmpty()) {
            return Collections.emptyMap();
        }
        Object claims = entity.get("claims");
        if (claims instanceof Map<?, ?> claimsMap) {
            return (Map<String, Object>) claimsMap;
        }
        return Collections.emptyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> sparqlSelect(String sparql) {
        try {
            String encoded = URLEncoder.encode(sparql, StandardCharsets.UTF_8);
            String url = properties.getSparqlEndpoint() + "?query=" + encoded + "&format=json";

            String body = fetchWithRetry(url);
            if (body == null) {
                return Collections.emptyList();
            }
            Map<String, Object> root = objectMapper.readValue(body, Map.class);
            Map<String, Object> results = (Map<String, Object>) root.get("results");
            if (results == null) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> bindings = (List<Map<String, Object>>) results.get("bindings");
            if (bindings == null) {
                return Collections.emptyList();
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (Map<String, Object> binding : bindings) {
                Map<String, String> row = new java.util.HashMap<>();
                for (Map.Entry<String, Object> entry : binding.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> valueMap) {
                        Object val = valueMap.get("value");
                        if (val != null) {
                            row.put(entry.getKey(), val.toString());
                        }
                    }
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("SPARQL query failed: {}", sparql, e);
            } else {
                LOGGER.warn("SPARQL query failed: {} - {}", e.getMessage(), sparql);
            }
            return Collections.emptyList();
        }
    }

    /**
     * Fetches the given URL with one retry when a 429 is received.
     *
     * @param url the URL to fetch
     * @return response body as string, or null on unrecoverable error
     */
    private String fetchWithRetry(String url) {
        try {
            return restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        if (resp.getStatusCode().value() == 429) {
                            handleRateLimitResponse(resp.getHeaders());
                        } else {
                            LOGGER.warn("Wikidata 4xx response {} for {}", resp.getStatusCode(), url);
                        }
                    })
                    .body(String.class);
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("HTTP fetch failed for {}", url, e);
            } else {
                LOGGER.warn("HTTP fetch failed for {}: {}", url, e.getMessage());
            }
            return null;
        }
    }

    private void handleRateLimitResponse(HttpHeaders headers) {
        String retryAfter = headers.getFirst("Retry-After");
        int waitSeconds = MAX_RETRY_AFTER_SECONDS;
        if (retryAfter != null) {
            try {
                waitSeconds = Math.min(Integer.parseInt(retryAfter.trim()), MAX_RETRY_AFTER_SECONDS);
            } catch (NumberFormatException ignored) {
            }
        }
        LOGGER.warn("Wikidata rate-limit (429). Waiting {} seconds before retry.", waitSeconds);
        try {
            Thread.sleep(Duration.ofSeconds(waitSeconds).toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildSitefilter(List<String> languages) {
        List<String> wikis = new ArrayList<>();
        for (String lang : languages) {
            wikis.add(lang + "wiki");
        }
        return String.join("|", wikis);
    }
}
