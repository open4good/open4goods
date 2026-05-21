package org.open4goods.services.wikidataservice.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.open4goods.services.wikidataservice.client.WikidataApiClient;
import org.open4goods.services.wikidataservice.config.WikidataServiceProperties;
import org.open4goods.services.wikidataservice.model.WikidataEntity;
import org.open4goods.services.wikidataservice.repository.WikidataEntityRepository;
import org.open4goods.services.wikidataservice.util.WikidataConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches Wikidata for entities matching a product by GTIN or brand+model.
 *
 * <p>Search order:
 * <ol>
 *   <li>Elasticsearch cache by GTIN (avoids SPARQL call when already resolved).</li>
 *   <li>SPARQL query by GTIN ({@code P3962}).</li>
 *   <li>Brand + model label search (when {@code brandModelFallbackEnabled}).</li>
 * </ol>
 */
public class WikidataSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikidataSearchService.class);

    private final WikidataApiClient apiClient;
    private final WikidataEntityRepository repository;
    private final WikidataLookupService lookupService;
    private final WikidataServiceProperties properties;

    public WikidataSearchService(
            WikidataApiClient apiClient,
            WikidataEntityRepository repository,
            WikidataLookupService lookupService,
            WikidataServiceProperties properties) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.lookupService = lookupService;
        this.properties = properties;
    }

    /**
     * Searches for a Wikidata entity matching the given GTIN.
     *
     * <p>Checks the ES cache first, then falls back to a SPARQL query.
     *
     * @param gtin the GTIN-13 value
     * @return matching entity, or empty when not found
     */
    public Optional<WikidataEntity> searchByGtin(String gtin) {
        if (gtin == null || gtin.isBlank()) {
            return Optional.empty();
        }
        String normalized = gtin.trim();

        List<WikidataEntity> cached = repository.findByGtins(normalized);
        if (!cached.isEmpty()) {
            LOGGER.debug("Wikidata GTIN cache hit for {}", normalized);
            return Optional.of(cached.get(0));
        }

        List<String> qIds = sparqlSearchByGtin(normalized);
        if (qIds.isEmpty()) {
            LOGGER.info("No Wikidata match for GTIN {}", normalized);
            return Optional.empty();
        }

        String qId = qIds.get(0);
        if (qIds.size() > 1) {
            LOGGER.warn("Multiple Wikidata matches for GTIN {}: {} — using first", normalized, qIds);
        }

        return lookupService.fetchByQid(qId);
    }

    /**
     * Searches for a Wikidata entity using brand and model label matching.
     *
     * <p>Tries each model candidate in turn, selecting the first result whose
     * brand label is compatible with the supplied brand.
     *
     * @param brand the product brand (may be null)
     * @param modelCandidates list of model identifiers ordered by priority
     * @return best matching entity, or empty when nothing is found
     */
    public Optional<WikidataEntity> searchByBrandModel(String brand, List<String> modelCandidates) {
        if (modelCandidates == null || modelCandidates.isEmpty()) {
            return Optional.empty();
        }
        int maxCandidates = Math.min(modelCandidates.size(), properties.getMaxModelCandidates());
        for (int i = 0; i < maxCandidates; i++) {
            String model = modelCandidates.get(i);
            List<String> qIds = sparqlSearchByBrandModel(brand, model);
            if (qIds.isEmpty()) {
                continue;
            }
            Optional<WikidataEntity> entity = lookupService.fetchByQid(qIds.get(0));
            if (entity.isPresent()) {
                LOGGER.info("Wikidata brand+model match for brand={} model={}: {}", brand, model, qIds.get(0));
                return entity;
            }
        }
        return Optional.empty();
    }

    /**
     * Directly executes a SPARQL query and returns results.
     * Exposed for admin/debug use.
     *
     * @param sparql SPARQL SELECT query string
     * @return result rows
     */
    public List<Map<String, String>> executeSparql(String sparql) {
        return apiClient.sparqlSelect(sparql);
    }

    private List<String> sparqlSearchByGtin(String gtin) {
        String sparql = "SELECT ?item WHERE { ?item wdt:" + WikidataConstants.P_GTIN + " \"" + escapeSparql(gtin) + "\" . }";
        List<Map<String, String>> rows = apiClient.sparqlSelect(sparql);
        List<String> qIds = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String itemUri = row.get("item");
            if (itemUri != null) {
                String qId = extractQid(itemUri);
                if (qId != null) {
                    qIds.add(qId);
                }
            }
        }
        return qIds;
    }

    private List<String> sparqlSearchByBrandModel(String brand, String model) {
        if (model == null || model.isBlank()) {
            return Collections.emptyList();
        }
        String safeModel = escapeSparql(model);
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ?item WHERE {\n");
        sb.append("  ?item rdfs:label|skos:altLabel \"").append(safeModel).append("\"@en .\n");
        if (brand != null && !brand.isBlank()) {
            String safeBrand = escapeSparql(brand);
            sb.append("  ?item wdt:").append(WikidataConstants.P_MANUFACTURER)
              .append("|wdt:").append(WikidataConstants.P_BRAND)
              .append(" ?brandItem .\n");
            sb.append("  ?brandItem rdfs:label \"").append(safeBrand).append("\"@en .\n");
        }
        sb.append("} LIMIT 5");
        String sparql = sb.toString();
        List<Map<String, String>> rows = apiClient.sparqlSelect(sparql);
        List<String> qIds = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String itemUri = row.get("item");
            if (itemUri != null) {
                String qId = extractQid(itemUri);
                if (qId != null) {
                    qIds.add(qId);
                }
            }
        }
        return qIds;
    }

    private String extractQid(String uri) {
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < uri.length() - 1) {
            return uri.substring(lastSlash + 1);
        }
        return null;
    }

    private String escapeSparql(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
