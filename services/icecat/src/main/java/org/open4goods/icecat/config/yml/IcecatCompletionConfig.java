package org.open4goods.icecat.config.yml;

import org.open4goods.model.localization.DomainLanguage;

/**
 * Configuration for the Icecat live API completion service.
 *
 * <p>The URL prefix must include the username and language parameters.
 * The GTIN is appended at request time. The language parameter should be
 * passed dynamically in production; this default is for backward compatibility.
 */
public class IcecatCompletionConfig {

    /** Default live API URL prefix (Open Icecat free tier, language must be appended dynamically). */
    private String iceCatUrlPrefix = "https://live.icecat.biz/api?UserName=openIcecat-live&Language=fr&GTIN=";

    /** Minimum delay in milliseconds between consecutive Icecat API calls (politeness / rate policy). */
    private Integer politenessDelayMs = 500;

    /** Language requested from the live API and used for language-tagged attributes, in the absence of a per-product language signal. */
    private DomainLanguage domainLanguage = DomainLanguage.fr;

    /** Number of days after a successful completion before a product is eligible for re-completion. */
    private Integer refreshIntervalDays = 30;

    /**
     * Number of days after a NOT_FOUND/RESTRICTED/ERROR outcome before a product is retried.
     * Kept shorter than {@link #refreshIntervalDays} would be wrong here since misses are not
     * expected to resolve quickly; same default, tune independently once volumetry is known.
     */
    private Integer negativeCacheIntervalDays = 30;

    /** Maximum number of attempts for a single live API call (initial attempt included), on timeout or 5xx only. */
    private Integer maxRetryAttempts = 3;

    /** Delay in milliseconds between retry attempts. */
    private Integer retryBackoffMs = 1000;

    /** Connect timeout in milliseconds for live API calls. */
    private Integer connectTimeoutMs = 5000;

    /** Read timeout in milliseconds for live API calls. */
    private Integer readTimeoutMs = 10000;

    public String getIceCatUrlPrefix() {
        return iceCatUrlPrefix;
    }

    public void setIceCatUrlPrefix(String iceCatUrlPrefix) {
        this.iceCatUrlPrefix = iceCatUrlPrefix;
    }

    public Integer getPolitenessDelayMs() {
        return politenessDelayMs;
    }

    public void setPolitenessDelayMs(Integer politenessDelayMs) {
        this.politenessDelayMs = politenessDelayMs;
    }

    public DomainLanguage getDomainLanguage() {
        return domainLanguage;
    }

    public void setDomainLanguage(DomainLanguage domainLanguage) {
        this.domainLanguage = domainLanguage;
    }

    public Integer getRefreshIntervalDays() {
        return refreshIntervalDays;
    }

    public void setRefreshIntervalDays(Integer refreshIntervalDays) {
        this.refreshIntervalDays = refreshIntervalDays;
    }

    public Integer getNegativeCacheIntervalDays() {
        return negativeCacheIntervalDays;
    }

    public void setNegativeCacheIntervalDays(Integer negativeCacheIntervalDays) {
        this.negativeCacheIntervalDays = negativeCacheIntervalDays;
    }

    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public Integer getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(Integer retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(Integer connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

}
