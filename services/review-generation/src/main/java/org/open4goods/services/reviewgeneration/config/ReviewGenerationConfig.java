package org.open4goods.services.reviewgeneration.config;

import java.time.Duration;
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


    /**
     * Flag to enable URL resolution in post-processing.
     * If true, the service will attempt to follow redirects (30x status) to resolve the final URL.
     */
    private boolean resolveUrl = true;
	private String batchFolder = "/opt/open4goods/batch-ids/";
    private int threadPoolSize = 10;
    private int maxQueueSize = 1000;  // Maximum size of the executor queue.
    private List<String> preferredDomains = new ArrayList<>();

    // Property used for building search queries.
    private String queryTemplate = "test %s \"%s\"";

    /**
     * Number of results requested for each search query.
     */
    private int searchResultsPerQuery = 10;

    /**
     * Google language restriction used for review-source discovery.
     */
    private String searchLanguageRestrict = "lang_fr";

    /**
     * Google country restriction used for review-source discovery.
     */
    private String searchCountryRestrict = "countryFR";

    /**
     * Google geo-location used for review-source discovery.
     */
    private String searchGeoLocation = "fr";

    /**
     * Google host language used for review-source discovery.
     */
    private String searchHostLanguage = "fr";

    /**
     * Safe-search mode used for review-source discovery.
     */
    private String searchSafe = "off";

    private List<String> excludedDomains = new ArrayList<>();

    // Limit the number of search queries.
    private int maxSearch = 5;


    // Minimum global tokens and source count required for valid generation.
    private int minGlobalTokens = 7500;
    private int minUrlCount = 4;



    // Properties for token-based content aggregation.
    private int maxTotalTokens = 100000;
    /**
     * Minimum markdown characters required before a fetch is considered usable.
     * This early gate rejects tiny placeholders before token estimation.
     */
    private int minMarkdownChars = 500;
    private int sourceMinTokens = 150;
    private int sourceMaxTokens = 10000;

    // Maximum concurrent URL fetch operations.
    private int maxConcurrentFetch = 3;

    /** Maximum URLs to fetch per product in retrieval pipeline. */
    private int maxUrlsPerProduct = 10;

    /** Maximum number of stored facts per product. */
    private int factsMaxStored = 40;

    /** Maximum markdown size per stored fact. */
    private int factMaxMarkdownChars = 20000;

    /**
     * Regex patterns for markdown lines to remove before token counting and prompt
     * injection. Patterns are applied line by line.
     */
    private List<String> markdownLineRemovalPatterns = new ArrayList<>(List.of(
            "(?i)^\\s*(menu|navigation|newsletter|footer|header)\\s*$",
            "(?i)^\\s*(abonnez-vous|inscrivez-vous|suivez-nous|partagez cet article).*$",
            "(?i)^\\s*(cookies?|gestion des cookies|politique de confidentialite|privacy policy).*$",
            "(?i)^\\s*(copyright|\\u00a9|tous droits reserves).*$"
    ));

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

    /**
     * Prompt key used for standard external-source review generation.
     */
    private String promptKey = "review-generation";

    /**
     * Prompt key used for model-native web search review generation.
     */
    private String groundedPromptKey = "review-generation-grounded";

    /**
     * Prompt key used for the first LLM phase: attribute extraction.
     * Only used when twoPhaseGeneration is true and retrievalMode is EXTERNAL_SOURCES.
     */
    private String attributeExtractionPromptKey = "review-generation-attributes";

    /**
     * Flag to enable the grounded prompt flow.
     */
    private boolean useGroundedPrompt = false;

    /**
     * When true, EXTERNAL_SOURCES generation uses two sequential LLM calls:
     * phase 1 extracts attributes, phase 2 generates the prose review using those attributes.
     * Grounded (MODEL_WEB_SEARCH) mode is always single-call regardless of this flag.
     */
    private boolean twoPhaseGeneration = true;

    /**
     * TTL in hours for entries in the in-memory process status map.
     * Completed (SUCCESS or FAILED) entries older than this are evicted by the scheduled cleanup.
     * Default is 24 hours.
     */
    private int statusMapTtlHours = 24;

    /**
     * Maximum number of entries kept in the URL resolution cache.
     */
    private int urlCacheMaxSize = 1000;

    /**
     * Interval between batch status scans for review generation jobs.
     */
    private Duration batchPollInterval = Duration.ofHours(1);

    /**
     * Cron schedule for daily batch review generation.
     */
    private String batchScheduleCron = "0 0 6 * * *";

    /**
     * Number of products to include in the scheduled batch.
     */
    private int batchScheduleSize = 20;

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

    public int getSearchResultsPerQuery() {
        return searchResultsPerQuery;
    }
    public void setSearchResultsPerQuery(int searchResultsPerQuery) {
        this.searchResultsPerQuery = searchResultsPerQuery;
    }

    public String getSearchLanguageRestrict() {
        return searchLanguageRestrict;
    }
    public void setSearchLanguageRestrict(String searchLanguageRestrict) {
        this.searchLanguageRestrict = searchLanguageRestrict;
    }

    public String getSearchCountryRestrict() {
        return searchCountryRestrict;
    }
    public void setSearchCountryRestrict(String searchCountryRestrict) {
        this.searchCountryRestrict = searchCountryRestrict;
    }

    public String getSearchGeoLocation() {
        return searchGeoLocation;
    }
    public void setSearchGeoLocation(String searchGeoLocation) {
        this.searchGeoLocation = searchGeoLocation;
    }

    public String getSearchHostLanguage() {
        return searchHostLanguage;
    }
    public void setSearchHostLanguage(String searchHostLanguage) {
        this.searchHostLanguage = searchHostLanguage;
    }

    public String getSearchSafe() {
        return searchSafe;
    }
    public void setSearchSafe(String searchSafe) {
        this.searchSafe = searchSafe;
    }

    public List<String> getExcludedDomains() {
        return excludedDomains;
    }

    public void setExcludedDomains(List<String> excludedDomains) {
        this.excludedDomains = excludedDomains;
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

    public int getMinMarkdownChars() {
        return minMarkdownChars;
    }
    public void setMinMarkdownChars(int minMarkdownChars) {
        this.minMarkdownChars = minMarkdownChars;
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
    public String getPromptKey() {
        return promptKey;
    }
    public void setPromptKey(String promptKey) {
        this.promptKey = promptKey;
    }
    public String getGroundedPromptKey() {
        return groundedPromptKey;
    }
    public void setGroundedPromptKey(String groundedPromptKey) {
        this.groundedPromptKey = groundedPromptKey;
    }
    public boolean isUseGroundedPrompt() {
        return useGroundedPrompt;
    }
    public void setUseGroundedPrompt(boolean useGroundedPrompt) {
        this.useGroundedPrompt = useGroundedPrompt;
    }
    public Duration getBatchPollInterval() {
        return batchPollInterval;
    }
    public void setBatchPollInterval(Duration batchPollInterval) {
        this.batchPollInterval = batchPollInterval;
    }
    public String getBatchScheduleCron() {
        return batchScheduleCron;
    }
    public void setBatchScheduleCron(String batchScheduleCron) {
        this.batchScheduleCron = batchScheduleCron;
    }
    public int getBatchScheduleSize() {
        return batchScheduleSize;
    }
    public void setBatchScheduleSize(int batchScheduleSize) {
        this.batchScheduleSize = batchScheduleSize;
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

	public int getMaxUrlsPerProduct() {
		return maxUrlsPerProduct;
	}
	public void setMaxUrlsPerProduct(int maxUrlsPerProduct) {
		this.maxUrlsPerProduct = maxUrlsPerProduct;
	}
	public int getFactsMaxStored() {
		return factsMaxStored;
	}
	public void setFactsMaxStored(int factsMaxStored) {
		this.factsMaxStored = factsMaxStored;
	}
	public int getFactMaxMarkdownChars() {
		return factMaxMarkdownChars;
	}
	public void setFactMaxMarkdownChars(int factMaxMarkdownChars) {
		this.factMaxMarkdownChars = factMaxMarkdownChars;
	}

	public List<String> getMarkdownLineRemovalPatterns() {
		return markdownLineRemovalPatterns;
	}
	public void setMarkdownLineRemovalPatterns(List<String> markdownLineRemovalPatterns) {
		this.markdownLineRemovalPatterns = markdownLineRemovalPatterns;
	}

	public boolean isResolveUrl() {
		return resolveUrl;
	}
	public void setResolveUrl(boolean resolveUrl) {
		this.resolveUrl = resolveUrl;
	}
	public String getBatchFolder() {
		return batchFolder;
	}
	public void setBatchFolder(String batchFolder) {
		this.batchFolder = batchFolder;
	}

    public String getAttributeExtractionPromptKey() {
        return attributeExtractionPromptKey;
    }
    public void setAttributeExtractionPromptKey(String attributeExtractionPromptKey) {
        this.attributeExtractionPromptKey = attributeExtractionPromptKey;
    }

    public boolean isTwoPhaseGeneration() {
        return twoPhaseGeneration;
    }
    public void setTwoPhaseGeneration(boolean twoPhaseGeneration) {
        this.twoPhaseGeneration = twoPhaseGeneration;
    }

    public int getStatusMapTtlHours() {
        return statusMapTtlHours;
    }
    public void setStatusMapTtlHours(int statusMapTtlHours) {
        this.statusMapTtlHours = statusMapTtlHours;
    }

    public int getUrlCacheMaxSize() {
        return urlCacheMaxSize;
    }
    public void setUrlCacheMaxSize(int urlCacheMaxSize) {
        this.urlCacheMaxSize = urlCacheMaxSize;
    }
}
