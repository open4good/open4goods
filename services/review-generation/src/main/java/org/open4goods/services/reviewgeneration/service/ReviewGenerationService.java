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
import org.open4goods.services.prompt.dto.PromptResponse;
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
 * This implementation has been refactored to use a new review generation logic:
 * <ol>
 *   <li>Before launching AI review generation, a preprocessing check is performed:
 *       <ul>
 *         <li>If the product does not have an associated AI review, generation proceeds.</li>
 *         <li>If a review exists, it is only regenerated if its creation date is older than the configured refresh delay,
 *             or (if the previous generation was unsuccessful) if the retry delay has passed.</li>
 *       </ul>
 *   </li>
 *   <li>It builds search queries using the product’s brand and model (and alternate models) with a query template.
 *       A maximum of {@code maxSearch} queries are performed.</li>
 *   <li>All search results are then sorted so that URLs containing preferred domains (configured via {@code preferredDomains})
 *       appear first.</li>
 *   <li>Next, URLs are fetched concurrently (up to {@code maxConcurrentFetch} tasks in parallel).
 *       For each fetched URL, the PromptService.estimateTokens() method is used to count tokens.
 *       Only pages with at least {@code sourceMinTokens} tokens are accepted and pages are aggregated until the total tokens
 *       reach {@code maxTotalTokens}.</li>
 *   <li>Finally, the collected markdown content is passed as prompt variables to generate the review.
 *       <br>
 *       <strong>New:</strong> After generation the text attributes of the generated review are precomputed to replace numeric references with corresponding HTML links.
 *   </li>
 * </ol>
 * <p>
 * After generating the review, the product is updated with a new AI review encapsulated in an AiReviewHolder (with sources, token counts, creation timestamp, and an “enoughData” flag) and re-indexed using {@code productRepository.forceIndex()}.
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
     * @param genAiService the Gen AI service.
     * @param meterRegistry the actuator meter registry.
     * @param productRepository the product repository service.
     */
    public ReviewGenerationService(ReviewGenerationConfig properties,
                                   GoogleSearchService googleSearchService,
                                   UrlFetchingService urlFetchingService,
                                   PromptService genAiService,
                                   MeterRegistry meterRegistry,
                                   ProductRepository productRepository) {
        this.properties = properties;
        this.googleSearchService = googleSearchService;
        this.urlFetchingService = urlFetchingService;
        this.genAiService = genAiService;
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
        // Initialize the instance-level URL fetch executor.
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
     * Synchronously generates a review for the given product and vertical configuration.
     * <p>
     * Preprocessing check: If the product already has an AI review and it is not older than the configured refresh delay,
     * the existing review is returned without triggering a new generation.
     *
     * @param product the product containing brand, primary model, and alternate models.
     * @param verticalConfig the vertical configuration.
     * @return the generated or existing AiReviewHolder.
     */
    public AiReviewHolder generateReviewSync(Product product, VerticalConfig verticalConfig) {
        long upc = product.getId();

        // Preprocessing: Check if review generation is necessary.
        if (!shouldGenerateReview(product)) {
            logger.info("Skipping AI review generation for UPC {} because an up-to-date review already exists.", upc);
            // TODO(p3,i18n) : i18n
            return product.getReviews().i18n("fr");
        }

        // Check duplicate submission for same GTIN.
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
            // Execute review generation and capture aggregated sources.
            GenerationResult genResult = executeReviewGeneration(product, verticalConfig, status);
            PromptResponse<AiReview> reviewResponse = genResult.response();
            AiReview newReview = reviewResponse.getBody();

            // Precompute update fields for text attributes.
            newReview = updateAiReviewReferences(newReview);

            // Build the holder with aggregated token information.
            holder.setReview(newReview);
            holder.setSources(genResult.sources());
            holder.setEnoughData(true);

            status.setResult(holder);
            status.setStatus(ReviewGenerationStatus.Status.SUCCESS);
            status.setEndTime(Instant.now().toEpochMilli());
            computeTimings(status);
            successfulCount.incrementAndGet();
            meterRegistry.counter("review.generation.success").increment();

        } catch (NotEnoughDataException e) {
            logger.error("Not enough data for review generation for UPC {}: {}", upc, e.getMessage(), e);
            status.setStatus(ReviewGenerationStatus.Status.FAILED);
            status.setErrorMessage(e.getMessage());
            status.setEndTime(Instant.now().toEpochMilli());
            computeTimings(status);
            meterRegistry.counter("review.generation.not_enough_data").increment();
        } catch (IOException | InterruptedException | ExecutionException | ResourceNotFoundException | SerialisationException e) {
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
        // Update product with the new AI review holder and force index update.
        // TODO(p3,i18n) : i18n
        product.getReviews().put("fr", holder);
        productRepository.forceIndex(product);
        return holder;
    }

    /**
     * Asynchronously initiates review generation for the given product and vertical configuration,
     * with an additional external pre-processing task.
     * <p>
     * If the optional {@code preProcessingFuture} is provided, the service sets the status to PREPROCESSING
     * and waits for its completion before proceeding with the review generation process (starting with the Google search).
     *
     * @param product the product containing brand, primary model, and alternate models.
     * @param verticalConfig the vertical configuration.
     * @param preProcessingFuture a CompletableFuture representing external arbitrary code to execute before starting generation.
     * @return the process ID (UPC) used to track generation status.
     */
    public long generateReviewAsync(Product product, VerticalConfig verticalConfig, CompletableFuture<Void> preProcessingFuture) {
        long upc = product.getId();

        // Preprocessing: Check if review generation is necessary.
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

        // Check duplicate submission for same GTIN.
        if (isActiveForGtin(product.gtin())) {
            throw new IllegalStateException("Review generation already in progress for product with GTIN " + product.gtin());
        }

        // Avoid duplicate runs on the same UPC if already processing.
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

        // Submit the review generation task asynchronously.
        executorService.submit(() -> {
            ReviewGenerationStatus status = processStatusMap.get(upc);
            AiReviewHolder holder = new AiReviewHolder();
            holder.setCreatedMs(Instant.now().toEpochMilli());
            try {
                // If an external pre-processing future is provided, execute it first.
                if (preProcessingFuture != null) {
                    status.setStatus(ReviewGenerationStatus.Status.PREPROCESSING);
                    status.addMessage("Executing external preprocessing step...");
                    preProcessingFuture.get();
                    status.addMessage("External preprocessing complete.");
                }
                // Continue with review generation.
                status.setStatus(ReviewGenerationStatus.Status.SEARCHING);
                GenerationResult genResult = executeReviewGeneration(product, verticalConfig, status);
                PromptResponse<AiReview> reviewResponse = genResult.response();
                AiReview newReview = reviewResponse.getBody();
                newReview = updateAiReviewReferences(newReview);
                
                holder.setReview(newReview);
                holder.setSources(genResult.sources());
                holder.setEnoughData(true);
                
                status.setResult(holder);
                status.setStatus(ReviewGenerationStatus.Status.SUCCESS);
                meterRegistry.counter("review.generation.success").increment();
            } catch ( NotEnoughDataException e) {
                logger.warn("Not enough data in asynchronous review generation for UPC {}: {}", upc, e.getMessage());
                status.setStatus(ReviewGenerationStatus.Status.FAILED);
                status.setErrorMessage(e.getMessage());
                meterRegistry.counter("review.generation.not_enough_data").increment();
            } catch (IOException | InterruptedException | ExecutionException | ResourceNotFoundException | SerialisationException e) {
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
            // TODO(p3,i18n): internationalisation
            product.getReviews().put("fr", holder);
            productRepository.forceIndex(product);
        });

        // Immediately after submission, update the status to QUEUED.
        int waiting = executorService.getQueue().size();
        ReviewGenerationStatus status = processStatusMap.get(upc);
        status.setStatus(ReviewGenerationStatus.Status.QUEUED);
        status.addMessage("Queued for execution. Number waiting in queue: " + waiting);

        return upc;
    }


    /**
     * Executes the review generation process using the new logic.
     * <ol>
     *   <li>Builds search queries using the product’s brand and primary model and then alternate models using the configured query template.
     *       A maximum of {@code maxSearch} queries are performed.</li>
     *   <li>Performs a Google search for each query and aggregates the search results.</li>
     *   <li>Deduplicates the search results to keep only one URL per distinct domain, and then sorts the results so that
     *       entries from preferred domains come first.</li>
     *   <li>Fetches the markdown content for each URL concurrently (using the instance-level fetchExecutor).
     *       For each fetched page, its token count is estimated via {@code PromptService.estimateTokens(String text)}.
     *       Pages with token counts below {@code sourceMinTokens} or above {@code sourceMaxTokens} are discarded.
     *       The pages are aggregated until the total token count reaches {@code maxTotalTokens}.</li>
     *   <li>Ensures that there is at least MIN_GLOBAL_TOKENS tokens and MIN_URL_COUNT sources. Otherwise, generation is aborted.</li>
     *   <li>Finally, the aggregated markdown content is passed as prompt variables to generate the review.</li>
     * </ol>
     * <p>
     * During the process, the provided {@code ReviewGenerationStatus} is updated with messages that track key steps.
     *
     * @param product the product.
     * @param verticalConfig the vertical configuration.
     * @param status the process status to update with processing messages.
     * @return a GenerationResult containing the prompt response and aggregated sources.
     * @throws InterruptedException 
     * @throws IOException 
     * @throws ExecutionException 
     * @throws SerialisationException 
     * @throws ResourceNotFoundException 
     * @throws NotEnoughDataException 
     * @throws Exception if an error occurs during generation.
     */
    private GenerationResult executeReviewGeneration(Product product, VerticalConfig verticalConfig, ReviewGenerationStatus status) throws IOException, InterruptedException, ExecutionException, ResourceNotFoundException, SerialisationException, NotEnoughDataException {
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
            // TODO(p3,i18N) : internationalisation from ReviewGenConfig
            GoogleSearchRequest searchRequest = new GoogleSearchRequest(query, "lang_fr", "countryFR");
            GoogleSearchResponse searchResponse = googleSearchService.search(searchRequest);
            searchesMade++;
            if (searchResponse != null && searchResponse.getResults() != null) {
                allResults.addAll(searchResponse.getResults());
            }
        }

        // Update status to FETCHING before starting URL fetch.
        status.setStatus(ReviewGenerationStatus.Status.FETCHING);

        List<GoogleSearchResult> sortedResults = allResults.stream()
                .filter(r -> r.getLink() != null && !r.getLink().isEmpty())
                // excluding pdf
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

        // Use the instance-level fetchExecutor instead of creating a new one.
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

        // Update status to ANALYSING before processing fetched content.
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

        // Check if sufficient data has been gathered.
        if (accumulatedTokens < properties.getMinGlobalTokens() || finalSourcesMap.size() < properties.getMinUrlCount()) {
            throw new NotEnoughDataException("Insufficient data for review generation: accumulatedTokens=" + accumulatedTokens + ", sources=" + finalSourcesMap.size());
        }
        
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
            if (attrConf.getSynonyms().size() > 0 ) {
                sb.append("        ").append("- ").append(attrConf.getKey()).append(" (").append(attrConf.getName().get("fr")).append(")").append("\n");				
            }
        });
        promptVariables.put("ATTRIBUTES", sb.toString());
		
        status.addMessage("AI generation");
        // Generate the review using the GenAiService with the prompt template "review-generation".
        PromptResponse<AiReview> ret = genAiService.objectPrompt("review-generation", promptVariables, AiReview.class);

        // Return the result containing the prompt response and aggregated tokens map.
        return new GenerationResult(ret, finalTokensMap, accumulatedTokens);
    }

    /**
     * Determines if a new AI review should be generated for the product.
     * <p>
     * Generation is required if:
     * <ul>
     *   <li>The product does not have an associated AI review.</li>
     *   <li>Or the existing review's creation date is older than the configured delay.
     *       The delay is based on whether the previous generation was successful (regenerationDelayDays)
     *       or not (retryDelayDays).</li>
     * </ul>
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
     * Computes and sets the duration and remaining time for a given process.
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
            status.setPercent(new Long(Math.round(Double.valueOf(duration) / Double.valueOf(estimated) * 100)).intValue());
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
     * Returns a predicate that filters duplicates based on a key.
     *
     * @param keyExtractor function extracting the key.
     * @param <T> type of elements.
     * @return predicate that returns true for first occurrence.
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
    
    /**
     * Replaces numeric references in the given text with corresponding HTML links.
     * For example, "[1]" becomes:
     * <a class="review-ref" href="#review-ref-1">[1]</a>.
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
     * Updates all textual fields of the provided AiReview by replacing numeric references with HTML links.
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
        
        List<String> pros = review.pros().stream()
                .map(this::replaceReferences)
                .toList();
        List<String> cons = review.cons().stream()
                .map(this::replaceReferences)
                .toList();
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
    
    /**
     * Inner record representing the result of a generation process.
     * It encapsulates the PromptResponse and the aggregated sources (mapping URLs to token counts).
     */
    private record GenerationResult(PromptResponse<AiReview> response, Map<String, Integer> sources, int accumulatedTokens) {}
}
