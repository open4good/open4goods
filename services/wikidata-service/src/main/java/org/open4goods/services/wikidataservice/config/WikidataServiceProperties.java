package org.open4goods.services.wikidataservice.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Wikidata completion service.
 *
 * <p>Bind via {@code wikidata.*} in application YAML/properties.
 */
@ConfigurationProperties(prefix = "wikidata")
public class WikidataServiceProperties {

    /**
     * User-Agent header sent with every Wikidata request.
     * Wikidata policy requires an identifiable UA with contact info.
     * Clients without one can be blocked.
     */
    private String userAgent = "nudger.fr/1.0 (https://nudger.fr; contact@nudger.fr) Spring-RestClient";

    /** SPARQL endpoint URL. */
    private String sparqlEndpoint = "https://query.wikidata.org/sparql";

    /** Wikidata MediaWiki API base URL used for wbgetentities calls. */
    private String restApiBase = "https://www.wikidata.org/w/api.php";

    /** Minimum delay in milliseconds between consecutive Wikidata API calls (politeness). */
    private int politenessDelayMs = 300;

    /**
     * Number of days before an already-fetched Wikidata entity is refreshed.
     * Products with a stored Q-id older than this are re-fetched.
     */
    private int refreshInDays = 30;

    /** Whether to attempt brand+model fallback when no GTIN match is found. */
    private boolean brandModelFallbackEnabled = true;

    /** Maximum number of model candidates to try in the brand+model fallback. */
    private int maxModelCandidates = 5;

    /**
     * BCP-47 language codes for which labels and descriptions are fetched.
     * Order determines priority in the DataFragment.
     */
    private List<String> languages = List.of("en", "fr", "de", "es", "it");

    /** Connection timeout for HTTP calls, in milliseconds. */
    private int connectTimeoutMs = 5000;

    /** Read timeout for HTTP calls, in milliseconds. */
    private int readTimeoutMs = 15000;

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public void setSparqlEndpoint(String sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
    }

    public String getRestApiBase() {
        return restApiBase;
    }

    public void setRestApiBase(String restApiBase) {
        this.restApiBase = restApiBase;
    }

    public int getPolitenessDelayMs() {
        return politenessDelayMs;
    }

    public void setPolitenessDelayMs(int politenessDelayMs) {
        this.politenessDelayMs = politenessDelayMs;
    }

    public int getRefreshInDays() {
        return refreshInDays;
    }

    public void setRefreshInDays(int refreshInDays) {
        this.refreshInDays = refreshInDays;
    }

    public boolean isBrandModelFallbackEnabled() {
        return brandModelFallbackEnabled;
    }

    public void setBrandModelFallbackEnabled(boolean brandModelFallbackEnabled) {
        this.brandModelFallbackEnabled = brandModelFallbackEnabled;
    }

    public int getMaxModelCandidates() {
        return maxModelCandidates;
    }

    public void setMaxModelCandidates(int maxModelCandidates) {
        this.maxModelCandidates = maxModelCandidates;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
