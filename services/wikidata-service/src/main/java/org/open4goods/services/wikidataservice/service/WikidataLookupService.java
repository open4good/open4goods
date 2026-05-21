package org.open4goods.services.wikidataservice.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.open4goods.services.wikidataservice.client.WikidataApiClient;
import org.open4goods.services.wikidataservice.config.WikidataServiceProperties;
import org.open4goods.services.wikidataservice.model.WikidataEntity;
import org.open4goods.services.wikidataservice.repository.WikidataEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches and caches Wikidata entities by Q-identifier.
 *
 * <p>On each lookup the Elasticsearch cache is checked first. When a cached
 * entity is present and younger than the configured refresh interval it is
 * returned directly. Otherwise the Wikidata REST API ({@code wbgetentities})
 * is called, the result parsed and the cache updated.
 */
public class WikidataLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikidataLookupService.class);

    private final WikidataApiClient apiClient;
    private final WikidataEntityRepository repository;
    private final WikidataParser parser;
    private final WikidataServiceProperties properties;

    public WikidataLookupService(
            WikidataApiClient apiClient,
            WikidataEntityRepository repository,
            WikidataParser parser,
            WikidataServiceProperties properties) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.parser = parser;
        this.properties = properties;
    }

    /**
     * Returns the {@link WikidataEntity} for the given Q-id.
     *
     * <p>Cached entities younger than the configured refresh interval are returned without
     * an API call. Stale or absent entries are fetched, parsed, and saved.
     *
     * @param qId the Wikidata Q-identifier, e.g. {@code Q12345}
     * @return the entity, or empty when the Q-id does not exist in Wikidata or the call fails
     */
    public Optional<WikidataEntity> fetchByQid(String qId) {
        if (qId == null || qId.isBlank()) {
            return Optional.empty();
        }

        Optional<WikidataEntity> cached = repository.findById(qId);
        if (cached.isPresent() && !isStale(cached.get())) {
            LOGGER.debug("Wikidata cache hit for {}", qId);
            return cached;
        }

        LOGGER.info("Fetching Wikidata entity {}", qId);
        Map<String, Object> entityMap = apiClient.getEntity(qId, properties.getLanguages());
        if (entityMap.isEmpty()) {
            LOGGER.warn("Wikidata returned no entity for {}", qId);
            return Optional.empty();
        }

        WikidataEntity entity = parser.parse(qId, entityMap);
        repository.save(entity);
        LOGGER.info("Saved Wikidata entity {} to cache", qId);
        return Optional.of(entity);
    }

    /**
     * Counts documents in the Wikidata entity index.
     *
     * @return document count
     */
    public long indexCount() {
        return repository.count();
    }

    private boolean isStale(WikidataEntity entity) {
        long refreshMs = Duration.ofDays(properties.getRefreshInDays()).toMillis();
        return System.currentTimeMillis() - entity.getLastFetchedAt() >= refreshMs;
    }
}
