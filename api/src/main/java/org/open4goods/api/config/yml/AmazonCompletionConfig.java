package org.open4goods.api.config.yml;

import java.time.Duration;

/**
 * Configuration for Amazon Product Advertising API completion.
 *
 * <p>The service is disabled by default because PA-API credentials are granted
 * only to Amazon Associates accounts and are rate limited. When enabled, the
 * completion service uses this configuration to search products by GTIN, refresh
 * known ASINs, and cache completion attempts on the product datasource code map.
 *
 */
public class AmazonCompletionConfig {

    private boolean enabled;
    private String accessKey;
    private String secretKey;
    private String partnerTag;
    private String host = "webservices.amazon.fr";
    private String region = "eu-west-1";
    private Duration sleepDuration = Duration.ofMillis(1100);
    private Duration refreshDuration = Duration.ofDays(30);
    private int maxCallsPerBatch = 8640;
    private String datasourceName = "amazon.fr.yml";
    private String searchIndex = "All";
    private String marketplace = "www.amazon.fr";

    /**
     * Creates disabled Amazon completion defaults for Amazon France.
     */
    public AmazonCompletionConfig() {
    }

    /**
     * Creates an Amazon completion configuration with explicit values.
     *
     * @param enabled whether Amazon completion can call PA-API
     * @param accessKey PA-API access key
     * @param secretKey PA-API secret key
     * @param partnerTag Amazon Associates tracking tag
     * @param host PA-API host, for example {@code webservices.amazon.fr}
     * @param region AWS signing region, for example {@code eu-west-1}
     * @param sleepDuration delay between PA-API calls
     * @param refreshDuration product-level cache duration before a new completion attempt
     * @param maxCallsPerBatch maximum products to process from Amazon batch endpoints
     * @param datasourceName datasource configuration file or datasource name for Amazon
     * @param searchIndex optional PA-API search index; defaults to all indexes
     * @param marketplace optional PA-API marketplace host, for example {@code www.amazon.fr}
     */
    public AmazonCompletionConfig(boolean enabled, String accessKey, String secretKey, String partnerTag,
            String host, String region, Duration sleepDuration, Duration refreshDuration, int maxCallsPerBatch,
            String datasourceName, String searchIndex, String marketplace) {
        setEnabled(enabled);
        setAccessKey(accessKey);
        setSecretKey(secretKey);
        setPartnerTag(partnerTag);
        setHost(host);
        setRegion(region);
        setSleepDuration(sleepDuration);
        setRefreshDuration(refreshDuration);
        setMaxCallsPerBatch(maxCallsPerBatch);
        setDatasourceName(datasourceName);
        setSearchIndex(searchIndex);
        setMarketplace(marketplace);
    }

    public boolean isConfigured() {
        return enabled
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && partnerTag != null && !partnerTag.isBlank()
                && host != null && !host.isBlank()
                && region != null && !region.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPartnerTag() {
        return partnerTag;
    }

    public void setPartnerTag(String partnerTag) {
        this.partnerTag = partnerTag;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host == null ? "webservices.amazon.fr" : host;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region == null ? "eu-west-1" : region;
    }

    public Duration getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(Duration sleepDuration) {
        this.sleepDuration = sleepDuration == null ? Duration.ofMillis(1100) : sleepDuration;
    }

    public Duration getRefreshDuration() {
        return refreshDuration;
    }

    public void setRefreshDuration(Duration refreshDuration) {
        this.refreshDuration = refreshDuration == null ? Duration.ofDays(30) : refreshDuration;
    }

    public int getMaxCallsPerBatch() {
        return maxCallsPerBatch;
    }

    public void setMaxCallsPerBatch(int maxCallsPerBatch) {
        this.maxCallsPerBatch = maxCallsPerBatch <= 0 ? 8640 : maxCallsPerBatch;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName == null ? "amazon.fr.yml" : datasourceName;
    }

    public String getSearchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(String searchIndex) {
        this.searchIndex = searchIndex == null ? "All" : searchIndex;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(String marketplace) {
        this.marketplace = marketplace == null ? "www.amazon.fr" : marketplace;
    }
}
