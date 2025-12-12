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

	
	
        private String batchFolder = "/opt/open4goods/batch-ids/";
    private int threadPoolSize = 10;
    private int maxQueueSize = 1000;  // Maximum size of the executor queue.
    private int maxPerIpPerDay = 3;
    private List<String> preferredDomains = new ArrayList<>();

    // Property used for building search queries.
    private String queryTemplate = "test %s \"%s\"";

    // Limit the number of search queries.
    private int maxSearch = 5;

    
    // Minimum global tokens and source count required for valid generation.
    private int minGlobalTokens = 7500;
    private int minUrlCount = 4;

    
    
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

    // ---------------------- New Properties ---------------------- //

    /**
     * Delay in days after which a successful AI review (enoughData == true) is considered outdated.
     * Default is 30 days.
     */
    private int regenerationDelayDays = 30;

    /**
     * Delay in days after which an unsuccessful AI review generation (enoughData == false) can be retried.
     * Default is 7 days.
     */
    private int retryDelayDays = 7;

    // ---------------------- Getters/Setters ---------------------- //

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

    public int getMaxPerIpPerDay() {
        return maxPerIpPerDay;
    }

    public void setMaxPerIpPerDay(int maxPerIpPerDay) {
        this.maxPerIpPerDay = maxPerIpPerDay;
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
    public void setMaxTotalTokens(int maxTotalTokens) {
        this.maxTotalTokens = maxTotalTokens;
    }

    public int getSourceMinTokens() {
        return sourceMinTokens;
    }
    public void setSourceMinTokens(int sourceMinTokens) {
        this.sourceMinTokens = sourceMinTokens;
    }

    public int getSourceMaxTokens() {
        return sourceMaxTokens;
    }
    public void setSourceMaxTokens(int sourceMaxTokens) {
        this.sourceMaxTokens = sourceMaxTokens;
    }

    public int getMaxConcurrentFetch() {
        return maxConcurrentFetch;
    }
    public void setMaxConcurrentFetch(int maxConcurrentFetch) {
        this.maxConcurrentFetch = maxConcurrentFetch;
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
    
    public int getRegenerationDelayDays() {
        return regenerationDelayDays;
    }
    public void setRegenerationDelayDays(int regenerationDelayDays) {
        this.regenerationDelayDays = regenerationDelayDays;
    }
    public int getRetryDelayDays() {
        return retryDelayDays;
    }
    public void setRetryDelayDays(int retryDelayDays) {
        this.retryDelayDays = retryDelayDays;
    }
	public int getMinGlobalTokens() {
		return minGlobalTokens;
	}
	public void setMinGlobalTokens(int minGlobalTokens) {
		this.minGlobalTokens = minGlobalTokens;
	}
	public int getMinUrlCount() {
		return minUrlCount;
	}
	public void setMinUrlCount(int minUrlCount) {
		this.minUrlCount = minUrlCount;
	}
	public String getBatchFolder() {
		return batchFolder;
	}
	public void setBatchFolder(String batchFolder) {
		this.batchFolder = batchFolder;
	}
    
    
}
