package org.open4goods.services.wikidataservice.client;

import java.util.List;
import java.util.Map;

/**
 * Low-level client for the Wikidata REST and SPARQL APIs.
 *
 * <p>All methods must honour the Wikidata polite-access contract:
 * set a proper User-Agent and respect 429 / Retry-After responses.
 */
public interface WikidataApiClient {

    /**
     * Fetches a Wikidata entity by Q-identifier using the {@code wbgetentities} action.
     *
     * @param qId the Q-identifier, e.g. {@code Q12345}
     * @param languages BCP-47 language codes for labels/descriptions/aliases
     * @return raw claims map as returned by the API, keyed by property ID;
     *         returns an empty map when the entity is not found or the call fails
     */
    Map<String, Object> getEntityClaims(String qId, List<String> languages);

    /**
     * Returns the full raw entity JSON map (labels, aliases, descriptions, claims, sitelinks)
     * for the given Q-identifier.
     *
     * @param qId the Q-identifier
     * @param languages BCP-47 language codes
     * @return raw entity map; empty when not found or on error
     */
    Map<String, Object> getEntity(String qId, List<String> languages);

    /**
     * Executes a SPARQL SELECT query against the Wikidata query service.
     *
     * @param sparql SPARQL query string
     * @return list of result rows, each row being a map of variable name to string value
     */
    List<Map<String, String>> sparqlSelect(String sparql);
}
