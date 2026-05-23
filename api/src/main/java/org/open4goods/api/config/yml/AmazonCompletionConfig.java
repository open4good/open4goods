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
public record AmazonCompletionConfig(
        boolean enabled,
        String accessKey,
        String secretKey,
        String partnerTag,
        String host,
        String region,
        Duration sleepDuration,
        Duration refreshDuration,
        int maxCallsPerBatch,
        String datasourceName,
        String searchIndex,
        String marketplace) {

    /**
     * Creates disabled Amazon completion defaults for Amazon France.
     */
    public AmazonCompletionConfig() {
        this(false, null, null, null, "webservices.amazon.fr", "eu-west-1",
                Duration.ofMillis(1100), Duration.ofDays(30), 8640, "amazon.fr.yml", "All", "www.amazon.fr");
    }

    /**
     * Applies conservative defaults when optional values are not configured.
     */
    public AmazonCompletionConfig {
        host = host == null ? "webservices.amazon.fr" : host;
        region = region == null ? "eu-west-1" : region;
        sleepDuration = sleepDuration == null ? Duration.ofMillis(1100) : sleepDuration;
        refreshDuration = refreshDuration == null ? Duration.ofDays(30) : refreshDuration;
        maxCallsPerBatch = maxCallsPerBatch <= 0 ? 8640 : maxCallsPerBatch;
        datasourceName = datasourceName == null ? "amazon.fr.yml" : datasourceName;
        searchIndex = searchIndex == null ? "All" : searchIndex;
        marketplace = marketplace == null ? "www.amazon.fr" : marketplace;
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

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getPartnerTag() {
        return partnerTag;
    }

    public String getHost() {
        return host;
    }

    public String getRegion() {
        return region;
    }

    public Duration getSleepDuration() {
        return sleepDuration;
    }

    public Duration getRefreshDuration() {
        return refreshDuration;
    }

    public int getMaxCallsPerBatch() {
        return maxCallsPerBatch;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public String getSearchIndex() {
        return searchIndex;
    }

    public String getMarketplace() {
        return marketplace;
    }
}
