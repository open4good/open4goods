package org.open4goods.services.reviewgeneration.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
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
 * It uses GoogleSearchService to fetch URLs based on the product’s brand and model,
 * applies a retry mechanism (using primary and alternate model names with iterative character removal),
 * fetches markdown content from the found URLs using UrlFetchingService,
 * and finally composes a prompt to generate the review using GenAiService.
 * <p>
 * The service supports both synchronous and asynchronous calls and tracks process status in memory.
 * Also, it implements HealthIndicator to report service health based on generation failures.
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
     * Executes the review generation process:
     * <ol>
     *   <li>Builds search queries using the product’s brand and model variations (primary and alternate),
     *       trying iterative character removal (up to configured max) until a result contains one of the preferred domains.</li>
     *   <li>For each query, performs a Google search and fetches the markdown content of each result.</li>
     *   <li>Aggregates the markdown content and composes a prompt which is sent to the GenAiService.</li>
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
	
	    // Map to collect source URL -> markdown content
	    Map<String, String> sourcesMap = new HashMap<>();
	
	    // Build a list of model names: primary first, then alternate models.
	    List<String> modelNames = new ArrayList<>();
	    modelNames.add(primaryModel);
	    if (alternateModels != null) {
	        modelNames.addAll(alternateModels);
	    }
	
	    // For each model name variant...
	    for (String modelName : modelNames) {
	        int callsMade = 0;
	        // Try iterative removal of trailing characters.
	        for (int removeCount = 0; removeCount <= properties.getMaxCharactersToRemove(); removeCount++) {
	            String modifiedModel = modelName;
	            if (removeCount > 0 && modelName.length() > removeCount) {
	                modifiedModel = modelName.substring(0, modelName.length() - removeCount);
	            }
	            // Use query template from config: e.g. "test %s \"%s\""
	            String query = String.format(properties.getQueryTemplate(), brand, modifiedModel);
	            logger.debug("Executing search query: {}", query);
	
	            // Create the search request using configured number of results.
	            GoogleSearchRequest searchRequest = new GoogleSearchRequest(query, properties.getNumberOfResults());
	            GoogleSearchResponse searchResponse = googleSearchService.search(searchRequest);
	            callsMade++;
	
	            // Process each search result.
	            searchResponse.getResults().forEach(result -> {
	                try {
	                    CompletableFuture<FetchResponse> fetchFuture = urlFetchingService.fetchUrl(result.getLink());
	                    FetchResponse fetchResponse = fetchFuture.get(); // wait for result
	                    if (fetchResponse != null && fetchResponse.markdownContent() != null 
	                        && !fetchResponse.markdownContent().isEmpty()) {
	                        // Maintain the map: source URL -> markdown content.
	                        sourcesMap.put(result.getLink(), fetchResponse.markdownContent());
	                    }
	                } catch (Exception e) {
	                    logger.warn("Failed to fetch content from URL {}: {}", result.getLink(), e.getMessage());
	                }
	            });
	
	            // Check if at least one search result contains a preferred domain.
	            boolean containsPreferredDomain = searchResponse.getResults().stream()
	                    .anyMatch(r -> properties.getPreferredDomains().stream()
	                            .anyMatch(domain -> r.getLink().contains(domain)));
	            if (containsPreferredDomain) {
	                logger.debug("Preferred domain found in search results for query: {}", query);
	                break; // exit the iterative removal loop for this model name
	            }
	            if (callsMade >= properties.getMaxSearchCalls()) {
	                logger.debug("Reached max search calls ({}) for model variant: {}", properties.getMaxSearchCalls(), modifiedModel);
	                break;
	            }
	        }
	    }
	
	    // Sorting and filtering sources:
	    // Keep only those with non-empty markdown content and length between min and max.
	    List<Map.Entry<String, String>> validSources = sourcesMap.entrySet().stream()
	            .filter(e -> {
	                String content = e.getValue();
	                int length = content.length();
	                return length >= properties.getMinMarkdownLength() && length <= properties.getMaxMarkdownLength();
	            })
	            // Sort: entries from preferred domains come first.
	            .sorted((e1, e2) -> {
	                boolean e1Preferred = properties.getPreferredDomains().stream().anyMatch(domain -> e1.getKey().contains(domain));
	                boolean e2Preferred = properties.getPreferredDomains().stream().anyMatch(domain -> e2.getKey().contains(domain));
	                if (e1Preferred && !e2Preferred) return -1;
	                if (!e1Preferred && e2Preferred) return 1;
	                return 0;
	            })
	            .toList();
	
	    if (validSources.size() > properties.getMaxSources()) {
	        logger.warn("Number of markdown sources ({}) exceeds the maximum limit ({}). Trimming extra sources.",
	                validSources.size(), properties.getMaxSources());
	        validSources = validSources.subList(0, properties.getMaxSources());
	    }
	
	    Map<String, String> finalSourcesMap = new HashMap<>();
	    validSources.forEach(e -> finalSourcesMap.put(e.getKey(), e.getValue()));
	
	    // Compose the prompt variables including the collected markdown content.
	    Map<String, Object> promptVariables = new HashMap<>();

	    promptVariables.put("sources", finalSourcesMap);
	    promptVariables.put("VERTICAL_NAME", verticalConfig.i18n("fr").getH1Title());
	    
	    
	    // Use the GenAiService with the prompt template "review-generation".
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
	}
