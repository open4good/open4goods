package org.open4goods.ui.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single URL that has been read from a sitemap
 * and then "checked" for status, timing, patterns, etc.
 */
@Document(indexName = "checked-urls") // <== Adjust index name as needed
public class CheckedUrl {

    /**
     * The URL as the document ID in Elasticsearch. Storing as keyword for exact matching.
     */
    @Id
    @Field(type = FieldType.Keyword)
    private String url;

    /**
     * Time of creation (first seen in sitemap), stored as a date in Elasticsearch.
     */
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private long created;

    /**
     * Last time any check was performed, stored as a date in Elasticsearch.
     */
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private long updated;

    /**
     * Last observed HTTP status code, e.g. 200, 404, 500, etc.
     */
    private int lastStatus;

    /**
     * Total request duration in milliseconds for the last check.
     */
    private long durationMillis;

    /**
     * Connection time (milliseconds) for establishing HTTP connection in the last check.
     */
    private long connectTimeMillis;

    /**
     * Whether the last check passed the health criteria.
     */
    private boolean healthCheckOk;

    /**
     * Any "bad patterns" encountered in the last check.
     */
    private Set<String> badPatternsEncountered = new HashSet<>();

    public CheckedUrl() {
    }

    public CheckedUrl(String url) {
        this.url = url;
        this.created = System.currentTimeMillis();
        this.updated = this.created;
    }

    // --- Getters / Setters ---

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public int getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(int lastStatus) {
        this.lastStatus = lastStatus;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public long getConnectTimeMillis() {
        return connectTimeMillis;
    }

    public void setConnectTimeMillis(long connectTimeMillis) {
        this.connectTimeMillis = connectTimeMillis;
    }

    public boolean isHealthCheckOk() {
        return healthCheckOk;
    }

    public void setHealthCheckOk(boolean healthCheckOk) {
        this.healthCheckOk = healthCheckOk;
    }

    public Set<String> getBadPatternsEncountered() {
        return badPatternsEncountered;
    }

    public void setBadPatternsEncountered(Set<String> badPatternsEncountered) {
        this.badPatternsEncountered = badPatternsEncountered;
    }

    @Override
    public String toString() {
        return "CheckedUrl{" +
                "url='" + url + '\'' +
                ", created=" + created +
                ", updated=" + updated +
                ", lastStatus=" + lastStatus +
                ", durationMillis=" + durationMillis +
                ", connectTimeMillis=" + connectTimeMillis +
                ", healthCheckOk=" + healthCheckOk +
                ", badPatternsEncountered=" + badPatternsEncountered +
                '}';
    }
}
