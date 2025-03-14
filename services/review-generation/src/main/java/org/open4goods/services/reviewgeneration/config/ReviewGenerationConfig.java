package org.open4goods.services.reviewgeneration.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the review generation service.
 */
@Configuration
@ConfigurationProperties(prefix = "review.generation")
public class ReviewGenerationConfig {

    private int threadPoolSize = 10;
    private int maxQueueSize = 100;  // Maximum size of the executor queue.
    private List<String> preferredDomains = new ArrayList<>();

    // Property used for building search queries.
    private String queryTemplate = "test %s \"%s\"";

    // Limit the number of search queries.
    private int maxSearch = 2;

    // Properties for token-based content aggregation.
    private int maxTotalTokens = 100000;
    private int sourceMinTokens = 150;
    private int sourceMaxTokens = 10000;

    // Maximum concurrent URL fetch operations.
    private int maxConcurrentFetch = 3;

    /**
     * The delay in months after which an existing AI review is considered outdated.
     * Default value is 6 months.
     */
    private int refreshDelayMonths = 6;

    /**
     * Estimated time for the review generation process.
     * Used to compute the remaining time.
     */
    private Long estimatedTime = 1000L * 60 * 2;

    // Getters and setters for existing properties.
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }
    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public List<String> getPreferredDomains() {
        return preferredDomains;
    }
    public void setPreferredDomains(List<String> preferredDomains) {
        this.preferredDomains = preferredDomains;
    }
    public String getQueryTemplate() {
        return queryTemplate;
    }
    public void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    public int getMaxSearch() {
        return maxSearch;
    }
    public void setMaxSearch(int maxSearch) {
        this.maxSearch = maxSearch;
    }
    public int getMaxTotalTokens() {
        return maxTotalTokens;
    }
    public void setMaxTotalTokens(int maxTokensPerRequest) {
        this.maxTotalTokens = maxTokensPerRequest;
    }
    public int getSourceMinTokens() {
        return sourceMinTokens;
    }
    public void setSourceMinTokens(int minTokens) {
        this.sourceMinTokens = minTokens;
    }
    public int getMaxConcurrentFetch() {
        return maxConcurrentFetch;
    }
    public void setMaxConcurrentFetch(int maxConcurrentFetch) {
        this.maxConcurrentFetch = maxConcurrentFetch;
    }
    public int getSourceMaxTokens() {
        return sourceMaxTokens;
    }
    public void setSourceMaxTokens(int sourceMaxTokens) {
        this.sourceMaxTokens = sourceMaxTokens;
    }

    public int getRefreshDelayMonths() {
        return refreshDelayMonths;
    }
    public void setRefreshDelayMonths(int refreshDelayMonths) {
        this.refreshDelayMonths = refreshDelayMonths;
    }
    
    public Long getEstimatedTime() {
        return estimatedTime;
    }
    
    public void setEstimatedTime(Long estimatedTime) {
        this.estimatedTime = estimatedTime;
    }
}
