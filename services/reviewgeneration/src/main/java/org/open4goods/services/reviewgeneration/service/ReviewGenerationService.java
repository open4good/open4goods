package org.open4goods.services.reviewgeneration.service;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.reviewgeneration.dto.ProcessStatus;
import org.open4goods.services.reviewgeneration.dto.ProcessStatus.Status;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service for generating AI-assisted reviews.
 * <p>
 * This implementation has been refactored to use a new review generation logic:
 * <ol>
 *   <li>It builds search queries using the product’s brand and model (and alternate models) with a query template.
 *       A maximum of {@code maxSearch} queries are performed.</li>
 *   <li>All search results are then sorted so that URLs containing preferred domains (configured via {@code preferredDomains})
 *       appear first.</li>
 *   <li>Next, URLs are fetched concurrently (up to {@code maxConcurrentFetch} tasks in parallel).
 *       For each fetched URL, the PromptService.estimateTokens() method is used to count tokens.
 *       Only pages with at least {@code minTokens} tokens are accepted and pages are aggregated until the total tokens
 *       reach {@code maxTokensPerRequest}.</li>
 *   <li>Finally, the collected markdown contents are passed as prompt variables to generate the review.</li>
 * </ol>
 * <p>
 * The service supports both synchronous and asynchronous calls and tracks process status in memory.
 * It also implements HealthIndicator to report service health based on generation failures.
 */
@Service
public class ReviewGenerationService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ReviewGenerationService.class);

    private final ReviewGenerationConfig properties;
    private final GoogleSearchService googleSearchService;
    private final UrlFetchingService urlFetchingService;
    private final PromptService genAiService;
    private final MeterRegistry meterRegistry;

    // Thread pool executor for asynchronous processing.
    private final ExecutorService executorService;

    // In-memory storage of process status keyed by UPC.
    private final ConcurrentMap<Long, ProcessStatus> processStatusMap = new ConcurrentHashMap<>();

    // Global metrics for review generations.
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger successfulCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);

    // Flag used by the health check: set to true if any generation failed.
    private volatile boolean lastGenerationFailed = false;

    /**
     * Constructs a new ReviewGenerationService.
     *
     * @param properties the review generation configuration properties.
     * @param googleSearchService the Google search service.
     * @param urlFetchingService the URL fetching service.
     * @param genAiService the Gen AI service.
     * @param meterRegistry the actuator meter registry.
     */
    public ReviewGenerationService(ReviewGenerationConfig properties,
                                   GoogleSearchService googleSearchService,
                                   UrlFetchingService urlFetchingService,
                                   PromptService genAiService,
                                   MeterRegistry meterRegistry) {
        this.properties = properties;
        this.googleSearchService = googleSearchService;
        this.urlFetchingService = urlFetchingService;
        this.genAiService = genAiService;
        this.meterRegistry = meterRegistry;
        this.executorService = Executors.newFixedThreadPool(properties.getThreadPoolSize());
    }

    /**
     * Synchronously generates a review for the given product and vertical configuration.
     *
     * @param product the product containing brand, primary model, and alternate models.
     * @param verticalConfig the vertical configuration.
     * @return the generated review.
     */
    public String generateReviewSync(Product product, VerticalConfig verticalConfig) {
        long upc = product.getId();
        ProcessStatus status = new ProcessStatus();
        status.setUpc(upc);
        status.setStatus(Status.PROCESSING);
        status.setStartTime(Instant.now());
        processStatusMap.put(upc, status);
        try {
            String review = executeReviewGeneration(product, verticalConfig);
            status.setResult(review);
            status.setStatus(Status.SUCCESS);
            status.setEndTime(Instant.now());
            successfulCount.incrementAndGet();
            meterRegistry.counter("review.generation.success").increment();
            return review;
        } catch (Exception e) {
            logger.error("Review generation failed for UPC {}: {}", upc, e.getMessage(), e);
            status.setStatus(Status.FAILED);
            status.setErrorMessage(e.getMessage());
            status.setEndTime(Instant.now());
            failedCount.incrementAndGet();
            meterRegistry.counter("review.generation.failed").increment();
            lastGenerationFailed = true;
            throw new RuntimeException("Review generation failed", e);
        } finally {
            totalProcessed.incrementAndGet();
        }
    }

    /**
     * Asynchronously initiates review generation for the given product and vertical configuration.
     *
     * @param product the product containing brand, primary model, and alternate models.
     * @param verticalConfig the vertical configuration.
     * @return the process ID (UPC) used to track generation status.
     */
    public long generateReviewAsync(Product product, VerticalConfig verticalConfig) {
        long upc = product.getId();
        // Avoid duplicate runs on the same UPC if already processing.
        processStatusMap.compute(upc, (key, existingStatus) -> {
            if (existingStatus != null && existingStatus.getStatus() == Status.PROCESSING) {
                return existingStatus;
            } else {
                ProcessStatus status = new ProcessStatus();
                status.setUpc(upc);
                status.setStatus(Status.PENDING);
                status.setStartTime(Instant.now());
                return status;
            }
        });

        // Submit the review generation task asynchronously.
        executorService.submit(() -> {
            ProcessStatus status = processStatusMap.get(upc);
            status.setStatus(Status.PROCESSING);
            try {
                String review = executeReviewGeneration(product, verticalConfig);
                status.setResult(review);
                status.setStatus(Status.SUCCESS);
                meterRegistry.counter("review.generation.success").increment();
            } catch (Exception e) {
                logger.error("Asynchronous review generation failed for UPC {}: {}", upc, e.getMessage(), e);
                status.setStatus(Status.FAILED);
                status.setErrorMessage(e.getMessage());
                meterRegistry.counter("review.generation.failed").increment();
                lastGenerationFailed = true;
            } finally {
                status.setEndTime(Instant.now());
                totalProcessed.incrementAndGet();
            }
        });
        return upc;
    }

    /**
     * Executes the review generation process using the new logic:
     * <ol>
     *   <li>Builds search queries using the product’s brand and primary model and then alternate models
     *       using the configured query template. A maximum of {@code maxSearch} queries are performed.</li>
     *   <li>Performs a Google search for each query and aggregates the search results.</li>
     *   <li>Deduplicates the search results to keep only one URL per distinct domain, and then sorts the results so that
     *       results from preferred domains appear first.</li>
     *   <li>Fetches the markdown content for each URL concurrently (up to {@code maxConcurrentFetch} tasks).
     *       For each fetched page, its token count is estimated via {@code PromptService.estimateTokens(String text)}.
     *       Pages with token counts below {@code minTokens} are discarded and pages are aggregated until
     *       the total token count reaches {@code maxTokensPerRequest}.</li>
     *   <li>Finally, the aggregated markdown content is passed as a prompt variable to generate the review.</li>
     * </ol>
     *
     * @param product the product.
     * @param verticalConfig the vertical configuration.
     * @return the generated review.
     * @throws Exception if an error occurs during generation.
     */
    private String executeReviewGeneration(Product product, VerticalConfig verticalConfig) throws Exception {
        String brand = product.brand();
        String primaryModel = product.model();
        Set<String> alternateModels = product.getAkaModels();

        // Build a list of search queries using the configured query template.
        List<String> queries = new ArrayList<>();
        queries.add(String.format(properties.getQueryTemplate(), brand, primaryModel));
        if (alternateModels != null) {
            for (String akaModel : alternateModels) {
                queries.add(String.format(properties.getQueryTemplate(), brand, akaModel));
            }
        }

        // Perform searches up to the maximum allowed (maxSearch).
        int searchesMade = 0;
        List<GoogleSearchResult> allResults = new ArrayList<>();
        int maxSearch = properties.getMaxSearch();
        for (String query : queries) {
            if (searchesMade >= maxSearch) {
                break;
            }
            logger.debug("Executing search query: {}", query);
            //TODO(p3,i18N) : internationalisation from ReviewGenConfig
            GoogleSearchRequest searchRequest = new GoogleSearchRequest(query, "lang_fr", "countryFR");
            GoogleSearchResponse searchResponse = googleSearchService.search(searchRequest);
            searchesMade++;
            if (searchResponse != null && searchResponse.getResults() != null) {
                allResults.addAll(searchResponse.getResults());
            }
        }

        // Deduplicate and sort the search results:
        // 1. Keep only one URL per distinct domain.
        // 2. Sort so that entries from preferred domains come first.
        List<GoogleSearchResult> sortedResults = allResults.stream()
                .filter(r -> r.getLink() != null && !r.getLink().isEmpty())
                
                .filter(distinctByKey(r -> {
                    try {
                        return new URL(r.getLink()).getHost();
                    } catch (Exception e) {
                        return r.getLink();
                    }
                }))
                .sorted((r1, r2) -> {
                    boolean r1Preferred = properties.getPreferredDomains().stream()
                            .anyMatch(domain -> r1.getLink().contains(domain));
                    boolean r2Preferred = properties.getPreferredDomains().stream()
                            .anyMatch(domain -> r2.getLink().contains(domain));
                    if (r1Preferred && !r2Preferred) return -1;
                    if (!r1Preferred && r2Preferred) return 1;
                    return 0;
                })
                .toList();

        // Prepare to fetch URL content concurrently using a dedicated executor.
        ExecutorService fetchExecutor = Executors.newFixedThreadPool(properties.getMaxConcurrentFetch());
        Map<String, CompletableFuture<FetchResponse>> fetchFutures = new HashMap<>();
        for (GoogleSearchResult result : sortedResults) {
            String url = result.getLink();
            CompletableFuture<FetchResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return urlFetchingService.fetchUrl(url).get();
                } catch (Exception e) {
                    logger.warn("Failed to fetch content from URL {}: {}", url, e.getMessage());
                    return null;
                }
            }, fetchExecutor);
            fetchFutures.put(url, future);
        }
        fetchExecutor.shutdown();

        // Aggregate fetched markdown content until the maximum token limit is reached.
        int maxTotalTokens = properties.getMaxTotalTokens();
        int minTokens = properties.getSourceMinTokens();
        int maxTokens = properties.getMaxTotalTokens();
        
        int accumulatedTokens = 0;
        Map<String, String> finalSourcesMap = new LinkedHashMap<>();
        Map<String, Integer> finalTokensMap = new LinkedHashMap<>();
        
        // Process the sorted URLs in order.
        for (GoogleSearchResult result : sortedResults) {
            String url = result.getLink();
            CompletableFuture<FetchResponse> future = fetchFutures.get(url);
            if (future == null) continue;
            FetchResponse fetchResponse = future.get();
            if (fetchResponse == null || fetchResponse.markdownContent() == null || fetchResponse.markdownContent().isEmpty()) {
                continue;
            }
            String content = fetchResponse.markdownContent();
            int tokenCount = genAiService.estimateTokens(content);
            if (tokenCount < minTokens) {
                logger.warn("Content from URL {} discarded due to insufficient tokens: {}", url, tokenCount);
                continue;
            }
            if (tokenCount > maxTokens) {
                logger.warn("Content from URL {} discarded, exceed tokens limit: {}", url, tokenCount);
                continue;
            }
            
            
            if (accumulatedTokens + tokenCount > maxTotalTokens) {
                logger.warn("Reached max tokens threshold. Current tokens: {}, URL tokens: {}, threshold: {}",
                        accumulatedTokens, tokenCount, maxTotalTokens);
                break;
            }
            finalSourcesMap.put(url, content);
            finalTokensMap.put(url, genAiService.estimateTokens(content));
            accumulatedTokens += tokenCount;
        }
        logger.info("Aggregated {} tokens from {} sources.", accumulatedTokens, finalSourcesMap.size());

        // Compose the prompt variables including the collected markdown content.
        Map<String, Object> promptVariables = new HashMap<>();
        promptVariables.put("sources", finalSourcesMap);
        promptVariables.put("tokens", finalTokensMap);
        
        promptVariables.put("PRODUCT_NAME", product.shortestOfferName());
        promptVariables.put("PRODUCT_BRAND", product.brand());
        promptVariables.put("PRODUCT_MODEL", product.model());
        promptVariables.put("PRODUCT_GTIN", product.gtin());
        promptVariables.put("PRODUCT", product);
		
		
        promptVariables.put("VERTICAL_NAME", verticalConfig.i18n("fr").getH1Title().getPrefix());

		StringBuilder sb = new StringBuilder();
		verticalConfig.getAttributesConfig().getConfigs().forEach(attrConf -> {
			if (attrConf.getSynonyms().size()>0 ) {
				sb.append("        ").append("- ").append(attrConf.getKey()).append(" (").append(attrConf.getName().get("fr")).append(")").append("\n");				
			}
			
		});
		promptVariables.put("ATTRIBUTES", sb.toString());
		
        // Generate the review using the GenAiService with the prompt template "review-generation".
        return genAiService.prompt("review-generation", promptVariables).getRaw();
    }

    /**
     * Returns the status of a review generation process by UPC.
     *
     * @param upc the UPC identifier.
     * @return the process status, or null if not found.
     */
    public ProcessStatus getProcessStatus(long upc) {
        return processStatusMap.get(upc);
    }

    /**
     * Returns global processing statistics.
     *
     * @return a map with keys: totalProcessed, successful, failed, and ongoing.
     */
    public Map<String, Integer> getGlobalStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("successful", successfulCount.get());
        stats.put("failed", failedCount.get());
        // Count processes that are still PENDING or PROCESSING.
        long ongoing = processStatusMap.values().stream()
                .filter(s -> s.getStatus() == Status.PENDING || s.getStatus() == Status.PROCESSING)
                .count();
        stats.put("ongoing", (int) ongoing);
        return stats;
    }

    /**
     * Health check implementation.
     * <p>
     * The service is reported as DOWN if any review generation failure has occurred.
     *
     * @return Health status UP if healthy; otherwise DOWN with error details.
     */
    @Override
    public Health health() {
        if (lastGenerationFailed) {
            return Health.down().withDetail("error", "One or more review generations have failed").build();
        }
        return Health.up().build();
    }

    /**
     * Returns a predicate that maintains state about what keys have been seen so far, filtering out duplicates.
     *
     * @param keyExtractor a function to extract the key for comparison.
     * @param <T> the type of the input elements.
     * @return a predicate that returns true if the element is encountered for the first time.
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
