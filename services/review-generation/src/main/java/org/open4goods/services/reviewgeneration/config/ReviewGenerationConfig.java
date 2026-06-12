package org.open4goods.services.reviewgeneration.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Optional vertical-specific preferred domains. When a product vertical has a
     * non-empty entry here, it replaces the global preferred-domain list for SERP
     * boosting and result ordering.
     */
    private Map<String, List<String>> preferredDomainsByVertical = Map.of();

    /**
     * Domains that must not be treated as manufacturer official pages even when
     * their host contains a brand token.
     */
    private List<String> officialUrlExcludedDomains = new ArrayList<>(List.of(
            "amazon.", "cdiscount.", "darty.", "fnac.", "galaxus.", "laredoute.", "rakuten.",
            "boulanger.", "electromenager-compare.", "lesmenagers.", "quel-lave-linge.", "nettoyant."));


    /**
     * Sibling-brand aliases used when judging source relevance. Many manufacturer groups
     * sell the exact same model under several brands (e.g. the BSH group rebadges the same
     * appliance as Bosch / Siemens / Neff / Gaggenau / Constructa), so the manufacturer
     * page for a Bosch GTIN may legitimately carry the Siemens brand in its title/URL.
     * Keys and values are matched case-insensitively; the relation is treated as symmetric.
     */
    private Map<String, List<String>> brandAliases = new java.util.HashMap<>(Map.of(
            "bosch", List.of("siemens", "neff", "gaggenau", "constructa"),
            "siemens", List.of("bosch", "neff", "gaggenau", "constructa"),
            "neff", List.of("bosch", "siemens", "gaggenau", "constructa"),
            "electrolux", List.of("aeg", "zanussi", "arthur martin", "faure"),
            "aeg", List.of("electrolux", "zanussi"),
            "whirlpool", List.of("indesit", "hotpoint", "ariston"),
            "indesit", List.of("whirlpool", "hotpoint", "ariston")));

    /**
     * Brand names that are known to be weak catalog labels for review-source search.
     * When one of these brands is present, offer/title evidence may promote a stronger
     * brand and model for search only.
     */
    private List<String> weakSearchBrands = new ArrayList<>();

    /**
     * Number of results requested for each search query.
     */
    private int searchResultsPerQuery = 10;

    /**
     * Google language restriction used for review-source discovery.
     * Set to "none" to rely solely on gl/hl soft bias and ranking tiers.
     */
    private String searchLanguageRestrict = "none";

    /**
     * Google country restriction used for review-source discovery.
     * Set to "none" to avoid hard-excluding EN authoritative sources such as rtings.com.
     */
    private String searchCountryRestrict = "none";

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

    /**
     * Total SERP query budget shared by all phases (primary, GTIN fallback,
     * low-quality fallback, partial retry). Default 5 — env-overridable via
     * REVIEW_GENERATION_SERP_BUDGET.
     */
    private int serpBudget = 5;


    // Minimum global tokens and source count required for valid generation.
    private int minGlobalTokens = 7500;
    private int minUrlCount = 4;

    /**
     * Optional vertical-specific fetch thresholds. These thresholds classify
     * fetched review evidence as complete, partial but usable, or failed.
     */
    private Map<String, FetchQualityThreshold> fetchThresholdsByVertical = Map.of(
            "tv", new FetchQualityThreshold(4000, 2, 2000, 1),
            "smartphone", new FetchQualityThreshold(6000, 3, 3000, 2),
            "smartphones", new FetchQualityThreshold(6000, 3, 3000, 2),
            "dishwasher", new FetchQualityThreshold(3000, 2, 1500, 1),
            "refrigerator", new FetchQualityThreshold(3000, 2, 1500, 1),
            "washing-machine", new FetchQualityThreshold(3000, 2, 1500, 1),
            "washing-machines", new FetchQualityThreshold(3000, 2, 1500, 1),
            "oven", new FetchQualityThreshold(3000, 2, 1500, 1),
            "air-conditioner", new FetchQualityThreshold(2000, 2, 1200, 1));



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
     * Prompt key used for the first LLM phase: attribute extraction.
     * Only used when twoPhaseGeneration is true.
     */
    private String attributeExtractionPromptKey = "review-generation-attributes";

    /**
     * When true, review generation uses two sequential LLM calls:
     * phase 1 extracts attributes, phase 2 generates the prose review using those attributes.
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

    public Map<String, List<String>> getPreferredDomainsByVertical() {
        return preferredDomainsByVertical;
    }
    public void setPreferredDomainsByVertical(Map<String, List<String>> preferredDomainsByVertical) {
        this.preferredDomainsByVertical = preferredDomainsByVertical == null ? Map.of() : preferredDomainsByVertical;
    }

    public List<String> getOfficialUrlExcludedDomains() {
        return officialUrlExcludedDomains;
    }
    public void setOfficialUrlExcludedDomains(List<String> officialUrlExcludedDomains) {
        this.officialUrlExcludedDomains = officialUrlExcludedDomains;
    }

    public Map<String, List<String>> getBrandAliases() {
        return brandAliases;
    }
    public void setBrandAliases(Map<String, List<String>> brandAliases) {
        this.brandAliases = brandAliases == null ? Map.of() : brandAliases;
    }

    public List<String> getWeakSearchBrands() {
        return weakSearchBrands;
    }
    public void setWeakSearchBrands(List<String> weakSearchBrands) {
        this.weakSearchBrands = weakSearchBrands == null ? new ArrayList<>() : weakSearchBrands;
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

    public int getSerpBudget() {
        return serpBudget;
    }
    public void setSerpBudget(int serpBudget) {
        this.serpBudget = serpBudget;
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

    public Map<String, FetchQualityThreshold> getFetchThresholdsByVertical() {
        return fetchThresholdsByVertical;
    }
    public void setFetchThresholdsByVertical(Map<String, FetchQualityThreshold> fetchThresholdsByVertical) {
        this.fetchThresholdsByVertical = fetchThresholdsByVertical == null ? Map.of() : fetchThresholdsByVertical;
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

    /**
     * Token and source-count thresholds for one vertical.
     */
    public static class FetchQualityThreshold {

        private int minGlobalTokens;
        private int minUrlCount;
        private int partialMinGlobalTokens;
        private int partialMinUrlCount;

        public FetchQualityThreshold() {
        }

        public FetchQualityThreshold(int minGlobalTokens, int minUrlCount, int partialMinGlobalTokens,
                int partialMinUrlCount) {
            this.minGlobalTokens = minGlobalTokens;
            this.minUrlCount = minUrlCount;
            this.partialMinGlobalTokens = partialMinGlobalTokens;
            this.partialMinUrlCount = partialMinUrlCount;
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

        public int getPartialMinGlobalTokens() {
            return partialMinGlobalTokens;
        }
        public void setPartialMinGlobalTokens(int partialMinGlobalTokens) {
            this.partialMinGlobalTokens = partialMinGlobalTokens;
        }

        public int getPartialMinUrlCount() {
            return partialMinUrlCount;
        }
        public void setPartialMinUrlCount(int partialMinUrlCount) {
            this.partialMinUrlCount = partialMinUrlCount;
        }
    }
}
