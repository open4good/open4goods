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

    private List<String> preferredDomains = new ArrayList<>();

    // Existing property used for building search queries.
    private String queryTemplate = "test %s \"%s\"";

    // Retained property to limit the number of search queries.
    private int maxSearch = 2;

    // New properties for token-based content aggregation.
    private int maxTotalTokens = 100000;
    private int sourceMinTokens = 150;
    private int sourceMaxTokens = 10000;

    // New property: maximum concurrent URL fetch operations.
    private int maxConcurrentFetch = 3;

    // Getters and setters for existing properties.
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
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

    // Getters and setters for new properties.
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
    
    
}
