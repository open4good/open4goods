package org.open4goods.services.wikidataservice.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Elasticsearch document mirroring a fetched Wikidata entity (Q-item).
 *
 * <p>Cached to avoid repeated calls to the Wikidata API. Refreshed when
 * {@code lastFetchedAt} is older than the configured refresh interval.
 */
@Document(indexName = "wikidata-entities")
public class WikidataEntity {

    /** Wikidata Q-identifier, e.g. "Q12345". */
    @Id
    private String qId;

    /**
     * GTINs associated with this item (P3962).
     * Multiple GTINs can point to the same Q-item.
     */
    @Field(type = FieldType.Keyword)
    private List<String> gtins = new ArrayList<>();

    /**
     * Multilingual labels in the form "lang:value", e.g. "en:Samsung Galaxy S23".
     * Stored as keyword list to allow exact-match filter by lang prefix.
     */
    @Field(type = FieldType.Keyword)
    private List<String> labels = new ArrayList<>();

    /**
     * Aliases (alternative names) in the same "lang:value" format.
     */
    @Field(type = FieldType.Keyword)
    private List<String> aliases = new ArrayList<>();

    /**
     * Multilingual descriptions in "lang:value" format.
     */
    @Field(type = FieldType.Keyword)
    private List<String> descriptions = new ArrayList<>();

    /**
     * Brand / manufacturer labels derived from P176 or P1716 claims.
     */
    @Field(type = FieldType.Keyword)
    private List<String> brandLabels = new ArrayList<>();

    /**
     * Image file names from P18 (main image), resolved via Commons.
     */
    @Field(type = FieldType.Keyword)
    private List<String> images = new ArrayList<>();

    /**
     * Video file names from P10, resolved via Commons.
     */
    @Field(type = FieldType.Keyword)
    private List<String> videos = new ArrayList<>();

    /**
     * Official website URL from P856.
     */
    @Field(type = FieldType.Keyword)
    private String website;

    /**
     * Release / publication year from P577 (YYYY).
     */
    @Field(type = FieldType.Keyword)
    private String releaseYear;

    /**
     * Wikipedia article URLs per language in "lang:url" format, e.g. "fr:https://fr.wikipedia.org/wiki/...".
     */
    @Field(type = FieldType.Keyword)
    private List<String> wikipediaUrls = new ArrayList<>();

    /**
     * Numeric/quantity claims from P2049 (width), P2048 (height), P2067 (mass), P2660 (depth), etc.
     * Key is the property ID (e.g. "P2049"), value is "amount unit" (e.g. "35.5 mm").
     */
    @Field(type = FieldType.Object, enabled = false)
    private Map<String, String> numericClaims = new HashMap<>();

    /** Epoch millis when this entity was last fetched from Wikidata. */
    @Field(type = FieldType.Long)
    private long lastFetchedAt;

    public String getQId() {
        return qId;
    }

    public void setQId(String qId) {
        this.qId = qId;
    }

    public List<String> getGtins() {
        return gtins;
    }

    public void setGtins(List<String> gtins) {
        this.gtins = gtins;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<String> descriptions) {
        this.descriptions = descriptions;
    }

    public List<String> getBrandLabels() {
        return brandLabels;
    }

    public void setBrandLabels(List<String> brandLabels) {
        this.brandLabels = brandLabels;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<String> getVideos() {
        return videos;
    }

    public void setVideos(List<String> videos) {
        this.videos = videos;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
    }

    public List<String> getWikipediaUrls() {
        return wikipediaUrls;
    }

    public void setWikipediaUrls(List<String> wikipediaUrls) {
        this.wikipediaUrls = wikipediaUrls;
    }

    public Map<String, String> getNumericClaims() {
        return numericClaims;
    }

    public void setNumericClaims(Map<String, String> numericClaims) {
        this.numericClaims = numericClaims;
    }

    public long getLastFetchedAt() {
        return lastFetchedAt;
    }

    public void setLastFetchedAt(long lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
    }
}
