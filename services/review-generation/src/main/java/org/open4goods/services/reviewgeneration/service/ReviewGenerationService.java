package org.open4goods.services.reviewgeneration.service;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.open4goods.model.ai.AiReview;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.dto.BatchPromptResponse;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationStatus;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationStatus.Status;
import org.open4goods.services.serialisation.exception.SerialisationException;
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
 * This implementation has been refactored to support two different review generation flows:
 * <ol>
 *   <li>The original realtime flow (using PromptService) for use cases that require immediate execution.</li>
 *   <li>A new batch flow (using BatchPromptService) that leverages OpenAI’s JSONL Batch API.
 *       On batch callback, the review is processed and the product is updated (data indexation).</li>
 * </ol>
 * <p>
 * In both cases, a preprocessing stage is performed (including Google searches, URL fetching, token counting,
 * and prompt variables composition) to aggregate markdown sources. Process status is tracked in memory,
 * and products are updated and re-indexed upon successful review generation.
 */
@Service
public class ReviewGenerationService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ReviewGenerationService.class);

    private final ReviewGenerationConfig properties;
    private final GoogleSearchService googleSearchService;
    private final UrlFetchingService urlFetchingService;
    // Real-time service for non-batch use cases.
    private final PromptService genAiService;
    // New batch service.
    private final BatchPromptService batchAiService;
    private final MeterRegistry meterRegistry;
    private final ProductRepository productRepository;

    // Thread pool executor for asynchronous processing.
    private final ThreadPoolExecutor executorService;
    // Instance-level thread pool for URL fetching.
    private final ThreadPoolExecutor fetchExecutor;
    // In-memory storage of process status keyed by UPC.
    private final ConcurrentMap<Long, ReviewGenerationStatus> processStatusMap = new ConcurrentHashMap<>();
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
     * @param genAiService the realtime PromptService.
     * @param batchAiService the new batch PromptService.
     * @param meterRegistry the actuator meter registry.
     * @param productRepository the product repository service.
     */
    public ReviewGenerationService(ReviewGenerationConfig properties,
                                   GoogleSearchService googleSearchService,
                                   UrlFetchingService urlFetchingService,
                                   PromptService genAiService,
                                   BatchPromptService batchAiService,
                                   MeterRegistry meterRegistry,
                                   ProductRepository productRepository) {
        this.properties = properties;
        this.googleSearchService = googleSearchService;
        this.urlFetchingService = urlFetchingService;
        this.genAiService = genAiService;
        this.batchAiService = batchAiService;
        this.meterRegistry = meterRegistry;
        this.productRepository = productRepository;
        this.executorService = new ThreadPoolExecutor(
            properties.getThreadPoolSize(),
            properties.getThreadPoolSize(),
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(properties.getMaxQueueSize()),
            new ThreadPoolExecutor.AbortPolicy()
        );
        this.fetchExecutor = new ThreadPoolExecutor(
            properties.getMaxConcurrentFetch(),
            properties.getMaxConcurrentFetch(),
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(properties.getMaxQueueSize()),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Checks if a review generation process for the given GTIN is already active.
     *
     * @param gtin the product GTIN.
     * @return true if an active process exists; false otherwise.
     */
    private boolean isActiveForGtin(String gtin) {
        return processStatusMap.values().stream()
                .anyMatch(s -> s.getGtin() != null && s.getGtin().equals(gtin)
                        && s.getStatus() != Status.SUCCESS && s.getStatus() != Status.FAILED);
    }

    /**
     * Determines if a new AI review should be generated for the product.
     * Generation is required if the product does not already have a review or if the existing review is outdated.
     *
     * @param product the product to check.
     * @return true if generation should proceed; false otherwise.
     */
    private boolean shouldGenerateReview(Product product) {
        AiReviewHolder existingReview = product.getReviews().i18n("fr");
        if (existingReview == null || existingReview.getCreatedMs() == null) {
            return true;
        }
        Instant reviewCreated = Instant.ofEpochMilli(existingReview.getCreatedMs());
        int delayDays = existingReview.isEnoughData() ? properties.getRegenerationDelayDays() : properties.getRetryDelayDays();
        Instant threshold = reviewCreated.plus(delayDays, ChronoUnit.DAYS);
        return Instant.now().isAfter(threshold);
    }

    /**
     * Prepares the prompt variables used for review generation.
     * This common code builds search queries, executes Google searches, fetches URLs concurrently,
     * and aggregates markdown sources and token counts.
     *
     * @param product the product.
     * @param verticalConfig the vertical configuration.
     * @param status the process status to update.
     * @return a map of prompt variables.
     * @throws IOException, InterruptedException, ExecutionException, SerialisationException, ResourceNotFoundException, NotEnoughDataException 
     */
    private Map<String, Object> preparePromptVariables(Product product, VerticalConfig verticalConfig, ReviewGenerationStatus status)
            throws IOException, InterruptedException, ExecutionException, ResourceNotFoundException, SerialisationException, NotEnoughDataException {
        String brand = product.brand();
        String primaryModel = product.model();
        Set<String> alternateModels = product.getAkaModels();

        // Build search queries.
        List<String> queries = new ArrayList<>();
        queries.add(String.format(properties.getQueryTemplate(), brand, primaryModel));
        if (alternateModels != null) {
            for (String akaModel : alternateModels) {
                queries.add(String.format(properties.getQueryTemplate(), brand, akaModel));
            }
        }

        status.addMessage("Searching the web...");
        int searchesMade = 0;
        List<GoogleSearchResult> allResults = new ArrayList<>();
        int maxSearch = properties.getMaxSearch();
        for (String query : queries) {
            if (searchesMade >= maxSearch) {
                break;
            }
            logger.debug("Executing search query: {}", query);
            status.addMessage("Executing search query: " + query);
            GoogleSearchRequest searchRequest = new GoogleSearchRequest(query, "lang_fr", "countryFR");
            GoogleSearchResponse searchResponse = googleSearchService.search(searchRequest);
            searchesMade++;
            if (searchResponse != null && searchResponse.getResults() != null) {
                allResults.addAll(searchResponse.getResults());
            }
        }

        status.setStatus(ReviewGenerationStatus.Status.FETCHING);

        // Sort and deduplicate results.
        List<GoogleSearchResult> sortedResults = allResults.stream()
                .filter(r -> r.getLink() != null && !r.getLink().isEmpty())
                .filter(r -> !r.getLink().endsWith(".pdf"))
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

        // Fetch URL contents concurrently.
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

        status.setStatus(ReviewGenerationStatus.Status.ANALYSING);
        status.addMessage("Fetching URL content concurrently...");

        int maxTotalTokens = properties.getMaxTotalTokens();
        int minTokens = properties.getSourceMinTokens();
        int maxTokens = properties.getSourceMaxTokens();

        int accumulatedTokens = 0;
        Map<String, String> finalSourcesMap = new LinkedHashMap<>();
        Map<String, Integer> finalTokensMap = new LinkedHashMap<>();

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
            finalTokensMap.put(url, tokenCount);
            accumulatedTokens += tokenCount;
            try {
                String domain = new URL(url).getHost();
                status.addMessage("Analysing " + domain);
            } catch (Exception e) {
                status.addMessage("Analysing " + url);
            }
        }
        logger.info("Aggregated {} tokens from {} sources.", accumulatedTokens, finalSourcesMap.size());

        // Check for minimum required data.
        if (accumulatedTokens < properties.getMinGlobalTokens() || finalSourcesMap.size() < properties.getMinUrlCount()) {
            throw new NotEnoughDataException("Insufficient data for review generation: accumulatedTokens=" + accumulatedTokens + ", sources=" + finalSourcesMap.size());
        }

        // Compose prompt variables.
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
            if (attrConf.getSynonyms().size() > 0) {
                sb.append("        ")
                  .append("- ")
                  .append(attrConf.getKey())
                  .append(" (")
                  .append(attrConf.getName().get("fr"))
                  .append(")")
                  .append("\n");
            }
        });
        promptVariables.put("ATTRIBUTES", sb.toString());
        status.addMessage("AI generation");

        // Store the aggregated tokens in the returned params, for conveniency
        // TOSO : use const
        promptVariables.put("TOTAL_TOKENS", accumulatedTokens);
        promptVariables.put("SOURCE_TOKENS", finalTokensMap);
        
        
        return promptVariables;
    }

    /**
     * Synchronous review generation using the realtime prompt service.
     * (Existing method; kept for backwards compatibility.)
     *
     * @param product the product.
     * @param verticalConfig the vertical configuration.
     * @return the generated or existing AiReviewHolder.
     */
    public AiReviewHolder generateReviewSync(Product product, VerticalConfig verticalConfig) {
        long upc = product.getId();

        if (!shouldGenerateReview(product)) {
            logger.info("Skipping AI review generation for UPC {} because an up-to-date review already exists.", upc);
            return product.getReviews().i18n("fr");
        }
        if (isActiveForGtin(product.gtin())) {
            throw new IllegalStateException("Review generation already in progress for product with GTIN " + product.gtin());
        }

        ReviewGenerationStatus status = new ReviewGenerationStatus();
        status.setUpc(upc);
        status.setGtin(product.gtin());
        status.setStatus(ReviewGenerationStatus.Status.PENDING);
        status.setStartTime(Instant.now().toEpochMilli());
        processStatusMap.put(upc, status);

        AiReviewHolder holder = new AiReviewHolder();
        holder.setCreatedMs(Instant.now().toEpochMilli());
        try {
            Map<String, Object> promptVariables = preparePromptVariables(product, verticalConfig, status);
            // Use the realtime prompt service.
            PromptResponse<AiReview> reviewResponse = genAiService.objectPrompt("review-generation", promptVariables, AiReview.class);
            AiReview newReview = reviewResponse.getBody();
            newReview = updateAiReviewReferences(newReview);
            holder.setReview(newReview);
            // TODO : share const
            holder.setSources((Map<String, Integer>) promptVariables.get("SOURCE_TOKENS")); // You may also choose to store additional aggregated data.
            holder.setTotalTokens((Integer) promptVariables.get("TOTAL_TOKENS")); // You may also choose to store additional aggregated data.
            
            holder.setEnoughData(true);
            status.setResult(holder);
            status.setStatus(ReviewGenerationStatus.Status.SUCCESS);
            status.setEndTime(Instant.now().toEpochMilli());
            computeTimings(status);
            successfulCount.incrementAndGet();
            meterRegistry.counter("review.generation.success").increment();
        } catch (Exception e) {
            logger.error("Review generation failed for UPC {}: {}", upc, e.getMessage(), e);
            status.setStatus(ReviewGenerationStatus.Status.FAILED);
            status.setErrorMessage(e.getMessage());
            status.setEndTime(Instant.now().toEpochMilli());
            computeTimings(status);
            failedCount.incrementAndGet();
            meterRegistry.counter("review.generation.failed").increment();
            lastGenerationFailed = true;
            throw new RuntimeException("Review generation failed", e);
        } finally {
            totalProcessed.incrementAndGet();
        }
        product.getReviews().put("fr", holder);
        productRepository.forceIndex(product);
        return holder;
    }

    /**
     * Synchronous review generation using the new batch service.
     * Instead of waiting for a synchronous API call, this method immediately returns a BatchPromptResponse.
     * A callback is registered on the batch future to perform product update and data indexation when complete.
     *
     * @param product the product.
     * @param verticalConfig the vertical configuration.
     * @return a BatchPromptResponse containing the job ID and a future that will complete with the prompt response.
     */
    public BatchPromptResponse<AiReview> generateReviewBatch(List<Product> products, VerticalConfig verticalConfig) {
        
    	List<Map<String, Object>> promptVariables = new ArrayList<>();
    	for (Product product : products) {
    		
	    	long upc = product.getId();
	        if (!shouldGenerateReview(product)) {
	            logger.info("Skipping AI review generation for UPC {} because an up-to-date review already exists.", upc);
	            // For batch method, one might return a dummy completed BatchPromptResponse.
	            
	            // TODO
	//            CompletableFuture<PromptResponse<AiReview>> dummy = CompletableFuture.completedFuture(product.getReviews().i18n("fr"));
	//            return new BatchPromptResponse<>("dummy", dummy);
	//            
	           continue;
	        }
	        if (isActiveForGtin(product.gtin())) {
	           continue;
	        }
	        ReviewGenerationStatus status = new ReviewGenerationStatus();
	        status.setUpc(upc);
	        status.setGtin(product.gtin());
	        status.setStatus(ReviewGenerationStatus.Status.PENDING);
	        status.setStartTime(Instant.now().toEpochMilli());
	        processStatusMap.put(upc, status);
	
	        // Prepare the prompt variables (includes search & aggregation).
	        try {
	            promptVariables.add(preparePromptVariables(product, verticalConfig, status));
	        } catch (Exception e) {
	            throw new RuntimeException("Failed to prepare prompt variables", e);
	        }
    	}

        
        
        // Submit batch job.
        BatchPromptResponse<String> batchResponse = batchAiService.batchPrompt("review-generation", promptVariables);
        // Register a callback for when the batch future completes.
        batchResponse.futureResponse().whenComplete((promptResponse, ex) -> {
            if (ex == null) {
//                try {
                	logger.error("OPEN AI Call back : {}, {}",promptResponse, ex);
                	
                	
                	System.out.println(promptResponse.getBody());
                	
//                    AiReview newReview = promptResponse.getBody();
//                    newReview = updateAiReviewReferences(newReview);
//                    AiReviewHolder holder = new AiReviewHolder();
//                    holder.setCreatedMs(Instant.now().toEpochMilli());
//                    holder.setReview(newReview);
//                 // TODO : share const
//                    holder.setSources((Map<String, Integer>) promptVariables.get("SOURCE_TOKENS")); // You may also choose to store additional aggregated data.
//                    holder.setTotalTokens((Integer) promptVariables.get("TOTAL_TOKENS")); // You may also choose to store additional aggregated data.
//                    
//                    holder.setEnoughData(true);
//                    status.setResult(holder);
//                    status.setStatus(ReviewGenerationStatus.Status.SUCCESS);
//                    status.setEndTime(Instant.now().toEpochMilli());
//                    computeTimings(status);
//                    successfulCount.incrementAndGet();
//                    meterRegistry.counter("review.generation.success").increment();
//                    // Update product review and force index.
//                    product.getReviews().put("fr", holder);
//                    productRepository.forceIndex(product);
//                } catch (Exception callbackEx) {
//                    logger.error("Error in batch callback for UPC {}: {}", upc, callbackEx.getMessage(), callbackEx);
//                }
            } else {
//                logger.error("Batch review generation failed for UPC {}: {}", upc, ex.getMessage(), ex);
//                status.setStatus(ReviewGenerationStatus.Status.FAILED);
//                status.setErrorMessage(ex.getMessage());
//                status.setEndTime(Instant.now().toEpochMilli());
//                computeTimings(status);
//                failedCount.incrementAndGet();
//                meterRegistry.counter("review.generation.failed").increment();
            }
            totalProcessed.incrementAndGet();
        });
        return null;
    }

    /**
     * Asynchronously initiates realtime review generation.
     * (Existing asynchronous method using PromptService.)
     *
     * @param product the product.
     * @param verticalConfig the vertical configuration.
     * @param preProcessingFuture an optional external preprocessing future.
     * @return the product UPC used to track generation status.
     */
    public long generateReviewAsync(Product product, VerticalConfig verticalConfig, CompletableFuture<Void> preProcessingFuture) {
        long upc = product.getId();
        if (!shouldGenerateReview(product)) {
            logger.info("Skipping asynchronous AI review generation for UPC {} because an up-to-date review already exists.", upc);
            ReviewGenerationStatus status = new ReviewGenerationStatus();
            status.setUpc(upc);
            status.setGtin(product.gtin());
            status.setStatus(ReviewGenerationStatus.Status.SUCCESS);
            status.addMessage("Existing valid AI review found. Skipping generation.");
            status.setStartTime(Instant.now().toEpochMilli());
            status.setEndTime(Instant.now().toEpochMilli());
            computeTimings(status);
            processStatusMap.put(upc, status);
            return upc;
        }
        if (isActiveForGtin(product.gtin())) {
            throw new IllegalStateException("Review generation already in progress for product with GTIN " + product.gtin());
        }
        processStatusMap.compute(upc, (key, existingStatus) -> {
            if (existingStatus != null && (existingStatus.getStatus() == ReviewGenerationStatus.Status.PENDING ||
                    existingStatus.getStatus() == ReviewGenerationStatus.Status.QUEUED ||
                    existingStatus.getStatus() == ReviewGenerationStatus.Status.SEARCHING ||
                    existingStatus.getStatus() == ReviewGenerationStatus.Status.FETCHING ||
                    existingStatus.getStatus() == ReviewGenerationStatus.Status.ANALYSING ||
                    existingStatus.getStatus() == ReviewGenerationStatus.Status.PREPROCESSING)) {
                return existingStatus;
            } else {
                ReviewGenerationStatus status = new ReviewGenerationStatus();
                status.setUpc(upc);
                status.setGtin(product.gtin());
                status.setStatus(ReviewGenerationStatus.Status.PENDING);
                status.setStartTime(Instant.now().toEpochMilli());
                return status;
            }
        });
        executorService.submit(() -> {
            ReviewGenerationStatus status = processStatusMap.get(upc);
            AiReviewHolder holder = new AiReviewHolder();
            holder.setCreatedMs(Instant.now().toEpochMilli());
            try {
                if (preProcessingFuture != null) {
                    status.setStatus(ReviewGenerationStatus.Status.PREPROCESSING);
                    status.addMessage("Executing external preprocessing step...");
                    preProcessingFuture.get();
                    status.addMessage("External preprocessing complete.");
                }
                status.setStatus(ReviewGenerationStatus.Status.SEARCHING);
                Map<String, Object> promptVariables = preparePromptVariables(product, verticalConfig, status);
                PromptResponse<AiReview> reviewResponse = genAiService.objectPrompt("review-generation", promptVariables, AiReview.class);
                AiReview newReview = reviewResponse.getBody();
                newReview = updateAiReviewReferences(newReview);
                holder.setReview(newReview);
             // TODO : share const
                holder.setSources((Map<String, Integer>) promptVariables.get("SOURCE_TOKENS")); // You may also choose to store additional aggregated data.
                holder.setTotalTokens((Integer) promptVariables.get("TOTAL_TOKENS")); // You may also choose to store additional aggregated data.
                
                holder.setEnoughData(true);
                status.setResult(holder);
                status.setStatus(ReviewGenerationStatus.Status.SUCCESS);
                meterRegistry.counter("review.generation.success").increment();
            } catch (Exception e) {
                logger.error("Asynchronous review generation failed for UPC {}: {}", upc, e.getMessage(), e);
                status.setStatus(ReviewGenerationStatus.Status.FAILED);
                status.setErrorMessage(e.getMessage());
                meterRegistry.counter("review.generation.failed").increment();
                lastGenerationFailed = true;
            } finally {
                status.setEndTime(Instant.now().toEpochMilli());
                computeTimings(status);
                totalProcessed.incrementAndGet();
            }
            product.getReviews().put("fr", holder);
            productRepository.forceIndex(product);
        });
        int waiting = executorService.getQueue().size();
        ReviewGenerationStatus status = processStatusMap.get(upc);
        status.setStatus(ReviewGenerationStatus.Status.QUEUED);
        status.addMessage("Queued for execution. Number waiting in queue: " + waiting);
        return upc;
    }

    /**
     * Computes and sets the duration and remaining time for a process based on start and end times.
     *
     * @param status the process status to update.
     */
    private void computeTimings(ReviewGenerationStatus status) {
        if (status.getStartTime() != null && status.getEndTime() != null) {
            long duration = status.getEndTime() - status.getStartTime();
            long estimated = properties.getEstimatedTime();
            long remaining = estimated - duration;
            status.setDuration(duration);
            status.setRemaining(remaining);
            status.setPercent((int) Math.round((double) duration / estimated * 100));
        }
    }

    /**
     * Dynamically updates timings for an ongoing process.
     *
     * @param status the process status to update.
     */
    private void updateDynamicTimings(ReviewGenerationStatus status) {
        if (status.getStartTime() != null && (status.getStatus() != ReviewGenerationStatus.Status.SUCCESS && status.getStatus() != ReviewGenerationStatus.Status.FAILED)) {
            long elapsed = Instant.now().toEpochMilli() - status.getStartTime();
            long estimated = properties.getEstimatedTime();
            long remaining = Math.max(estimated - elapsed, 0);
            int percent = (int) Math.min(100, Math.round((double) elapsed / estimated * 100));
            status.setDuration(elapsed);
            status.setRemaining(remaining);
            status.setPercent(percent);
        }
    }

    /**
     * Returns the status of a review generation process by UPC.
     *
     * @param upc the UPC identifier.
     * @return the process status, or null if not found.
     */
    public ReviewGenerationStatus getProcessStatus(long upc) {
        ReviewGenerationStatus status = processStatusMap.get(upc);
        if (status != null && status.getStatus() != ReviewGenerationStatus.Status.SUCCESS && status.getStatus() != ReviewGenerationStatus.Status.FAILED) {
            updateDynamicTimings(status);
        }
        return status;
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
        long ongoing = processStatusMap.values().stream()
                .filter(s -> s.getStatus() == ReviewGenerationStatus.Status.PENDING ||
                             s.getStatus() == ReviewGenerationStatus.Status.QUEUED ||
                             s.getStatus() == ReviewGenerationStatus.Status.SEARCHING ||
                             s.getStatus() == ReviewGenerationStatus.Status.FETCHING ||
                             s.getStatus() == ReviewGenerationStatus.Status.ANALYSING)
                .count();
        stats.put("ongoing", (int) ongoing);
        return stats;
    }

    /**
     * Health check implementation.
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
     * Returns a predicate to filter distinct elements based on a key extractor.
     *
     * @param keyExtractor a function to extract the key.
     * @param <T> the element type.
     * @return a predicate that yields true only for the first occurrence.
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /**
     * Replaces numeric references in the given text with corresponding HTML links.
     *
     * @param text the original text.
     * @return text with HTML link replacements.
     */
    private String replaceReferences(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\[(\\d+)\\]", "<a class=\"review-ref\" href=\"#review-ref-$1\">[$1]</a>");
    }

    /**
     * Updates textual fields in the provided AiReview by replacing numeric references with HTML links.
     *
     * @param review the original AiReview.
     * @return a new AiReview with updated text fields.
     */
    private AiReview updateAiReviewReferences(AiReview review) {
        String description = replaceReferences(review.description());
        String shortDescription = replaceReferences(review.shortDescription());
        String mediumTitle = replaceReferences(review.mediumTitle());
        String shortTitle = replaceReferences(review.shortTitle());
        String technicalReview = replaceReferences(review.technicalReview());
        String ecologicalReview = replaceReferences(review.ecologicalReview());
        String summary = replaceReferences(review.summary());
        String dataQuality = replaceReferences(review.dataQuality());
        List<String> pros = review.pros().stream().map(this::replaceReferences).toList();
        List<String> cons = review.cons().stream().map(this::replaceReferences).toList();
        List<AiReview.AiSource> sources = review.sources().stream()
                .map(source -> new AiReview.AiSource(
                        source.number(),
                        replaceReferences(source.name()),
                        replaceReferences(source.description()),
                        source.url()
                ))
                .toList();
        List<AiReview.AiAttribute> attributes = review.attributes().stream()
                .map(attr -> new AiReview.AiAttribute(
                        replaceReferences(attr.name()),
                        replaceReferences(attr.value())
                ))
                .toList();
        return new AiReview(
                description,
                shortDescription,
                mediumTitle,
                shortTitle,
                technicalReview,
                ecologicalReview,
                summary,
                pros,
                cons,
                sources,
                attributes,
                dataQuality
        );
    }

   
}
