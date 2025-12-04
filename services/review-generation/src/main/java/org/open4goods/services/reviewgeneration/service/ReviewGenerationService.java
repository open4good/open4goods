package org.open4goods.services.reviewgeneration.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
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
import org.open4goods.model.ai.AiReview.AiSource;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.review.ReviewGenerationStatus.Status;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.exception.GoogleSearchException;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.dto.openai.BatchOutput;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service for generating AI-assisted reviews.
 * <p>
 * This implementation supports two review generation flows:
 * <ol>
 *   <li>A realtime flow (using PromptService) that executes immediately.</li>
 *   <li>A batch flow (using BatchPromptService) that leverages OpenAI’s JSONL Batch API.
 *       In the batch flow, after submitting the job, a tracking file is written with the job ID and product identifiers.
 *       A scheduled method later scans the tracking folder; for each completed job, it retrieves the output and
 *       updates the corresponding products.</li>
 * </ol>
 * <p>
 * In both cases, a common preprocessing stage is executed: search queries are generated and executed via GoogleSearchService,
 * URL content is fetched concurrently via UrlFetchingService, markdown sources and token counts are aggregated,
 * and prompt variables are composed.
 * </p>
 */
@Service
public class ReviewGenerationService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ReviewGenerationService.class);

    private final ReviewGenerationConfig properties;
    private final GoogleSearchService googleSearchService;
    private final UrlFetchingService urlFetchingService;
    // Realtime prompt service for immediate generation.
    private final PromptService genAiService;
    // Batch prompt service for batch processing.
    private final BatchPromptService batchAiService;
    private final MeterRegistry meterRegistry;
    private final ProductRepository productRepository;

    // Thread pool executors.
    private final ThreadPoolExecutor executorService;
    private final ThreadPoolExecutor fetchExecutor;
    // In-memory storage of process statuses (keyed by UPC).
    private final ConcurrentMap<Long, ReviewGenerationStatus> processStatusMap = new ConcurrentHashMap<>();
    // Global metrics.
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger successfulCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    // Health flag.
    private volatile boolean lastGenerationFailed = false;

    private final ConcurrentMap<String, AtomicInteger> ipGenerationCounts = new ConcurrentHashMap<>();
    private LocalDate generationCountDate = LocalDate.now();
    private final Object ipQuotaLock = new Object();

    // New fields for batch tracking.
    private final File trackingFolder;
    private final ObjectMapper objectMapper;


    /**
     * Constructs a new ReviewGenerationService.
     *
     * @param properties the review generation configuration properties.
     * @param googleSearchService the Google search service.
     * @param urlFetchingService the URL fetching service.
     * @param genAiService the realtime PromptService.
     * @param batchAiService the batch PromptService.
     * @param meterRegistry the actuator MeterRegistry.
     * @param productRepository the product repository service.
     */
    public ReviewGenerationService(ReviewGenerationConfig properties,
                                   GoogleSearchService googleSearchService,
                                   UrlFetchingService urlFetchingService,
                                   PromptService genAiService,
                                   BatchPromptService batchAiService,
                                   MeterRegistry meterRegistry,
                                   ProductRepository productRepository

    		) {
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
            properties.getMaxQueueSize(),
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(properties.getMaxQueueSize()*10),
            new ThreadPoolExecutor.AbortPolicy()
        );
        // Initialize the tracking folder based on configuration (or derive from batch folder).
        String trackingPath = properties.getBatchFolder() + File.separator + "tracking";
        this.trackingFolder = new File(trackingPath);
        if (!this.trackingFolder.exists()) {
            this.trackingFolder.mkdirs();
        }
        this.objectMapper = new ObjectMapper();
    }

    // -------------------- Preprocessing Logic -------------------- //

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
     * <p>
     * This common code builds search queries from the product’s brand and model,
     * performs Google searches via GoogleSearchService, fetches URL content concurrently via UrlFetchingService,
     * aggregates markdown sources along with token counts, and finally composes the prompt variables.
     * </p>
     *
     * @param product the product.
     * @param verticalConfig the vertical configuration.
     * @param status the process status to update.
     * @return a map of prompt variables.
     * @throws IOException, InterruptedException, ExecutionException, SerialisationException, ResourceNotFoundException, NotEnoughDataException
     * @throws GoogleSearchException
     */
    private Map<String, Object> preparePromptVariables(Product product, VerticalConfig verticalConfig, ReviewGenerationStatus status)
            throws IOException, InterruptedException, ExecutionException, ResourceNotFoundException, SerialisationException, NotEnoughDataException, GoogleSearchException {
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
            if (searchResponse != null && searchResponse.results() != null) {
                allResults.addAll(searchResponse.results());
            }
        }

        status.setStatus(ReviewGenerationStatus.Status.FETCHING);

        // Sort and deduplicate results.
        List<GoogleSearchResult> sortedResults = allResults.stream()
                .filter(r -> r.link() != null && !r.link().isEmpty())
                .filter(r -> !r.link().endsWith(".pdf"))
                .filter(distinctByKey(r -> {
                    try {
                        return new URL(r.link()).getHost();
                    } catch (Exception e) {
                        return r.link();
                    }
                }))
                .sorted((r1, r2) -> {
                    boolean r1Preferred = properties.getPreferredDomains().stream()
                            .anyMatch(domain -> r1.link().contains(domain));
                    boolean r2Preferred = properties.getPreferredDomains().stream()
                            .anyMatch(domain -> r2.link().contains(domain));
                    if (r1Preferred && !r2Preferred) return -1;
                    if (!r1Preferred && r2Preferred) return 1;
                    return 0;
                })
                .toList();

        // Fetch URL contents concurrently.
        Map<String, CompletableFuture<FetchResponse>> fetchFutures = new HashMap<>();
        for (GoogleSearchResult result : sortedResults) {
            String url = result.link();
            CompletableFuture<FetchResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return urlFetchingService.fetchUrlAsync(url).get();
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
            String url = result.link();
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

        // Store aggregated tokens for convenience.
        promptVariables.put("TOTAL_TOKENS", accumulatedTokens);
        promptVariables.put("SOURCE_TOKENS", finalTokensMap);

        return promptVariables;
    }


    /**
     * Asynchronous review generation using the realtime prompt service.
     * (Uses a ThreadPoolExecutor to run the process asynchronously.)
     *
     * @param product the product.
     * @param verticalConfig the vertical configuration.
     * @param preProcessingFuture an optional external preprocessing future.
     * @return the product UPC used to track generation status.
     */
    private void resetIpGenerationCountersIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!generationCountDate.isEqual(today)) {
            ipGenerationCounts.clear();
            generationCountDate = today;
        }
    }

    private void enforceIpQuota(String clientIp) {
        String ipKey = StringUtils.hasText(clientIp) ? clientIp : "unknown";
        synchronized (ipQuotaLock) {
            resetIpGenerationCountersIfNeeded();
            int used = ipGenerationCounts.getOrDefault(ipKey, new AtomicInteger(0)).get();
            int maxDaily = properties.getMaxPerIpPerDay();
            if (used >= maxDaily) {
                logger.warn("IP {} reached daily AI review generation limit ({}).", ipKey, maxDaily);
                throw new ReviewGenerationQuotaExceededException(ipKey, maxDaily);
            }
            ipGenerationCounts.computeIfAbsent(ipKey, key -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    public long generateReviewAsync(Product product, VerticalConfig verticalConfig, CompletableFuture<Void> preProcessingFuture,
            String clientIp) {
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
        enforceIpQuota(clientIp);
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

            	// Populate attributes
            	populateAttributes(product, newReview);

                holder.setReview(newReview);
                holder.setSources((Map<String, Integer>) promptVariables.get("SOURCE_TOKENS"));
                holder.setTotalTokens((Integer) promptVariables.get("TOTAL_TOKENS"));
                holder.setEnoughData(true);
                status.setResult(holder);
                status.setStatus(ReviewGenerationStatus.Status.SUCCESS);
                meterRegistry.counter("review.generation.success").increment();
            } catch (NotEnoughDataException e) {
                logger.warn("Not enought data for generating ai review for {}", product);
                holder.setEnoughData(false);
                status.setResult(holder);
                status.setStatus(ReviewGenerationStatus.Status.FAILED);
                status.setErrorMessage("Not enough data to generate an AI review for this product.");
                meterRegistry.counter("review.generation.failed").increment();
                lastGenerationFailed = true;
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

    // -------------------- Batch Review Generation Methods -------------------- //

    /**
     * Submits a batch review generation request.
     * <p>
     * For each eligible product, it prepares prompt variables (including Google searches,
     * URL fetching, token counting, etc.) and collects the product GTINs.
     * Then it submits a batch job via the BatchPromptService using the prompt key "review-generation".
     * Instead of using a future callback, a tracking file is written to a designated folder so that a scheduled
     * task can later check job status and process the results.
     * </p>
     *
     * @param products the list of products to process.
     * @param verticalConfig the vertical configuration.
     * @return the batch job ID.
     */
    public String generateReviewBatchRequest(List<Product> products, VerticalConfig verticalConfig) {
        List<Map<String, Object>> promptVariablesList = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Product product : products) {
            long upc = product.getId();
            if (!shouldGenerateReview(product)) {
                logger.info("Skipping AI review generation for UPC {} because an up-to-date review already exists.", upc);
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
            try {
                promptVariablesList.add(preparePromptVariables(product, verticalConfig, status));
                ids.add(product.gtin());
            } catch (NotEnoughDataException e) {
                logger.error("Not enough data while preparing prompt for UPC {}: {}", product.getId(), e.getMessage());
                AiReviewHolder holder = new AiReviewHolder();
                holder.setCreatedMs(Instant.now().toEpochMilli());
                holder.setEnoughData(false);
                product.getReviews().put("fr", holder);
                productRepository.index(product);
            } catch (Exception e) {
                logger.error("Error while preparing prompt for UPC {}: {}", product.getId(), e.getMessage());
            }
        }
        if (promptVariablesList.isEmpty()) {
            logger.info("No new products for batch review generation.");
            return "NoBatchJob";
        }
        // Submit batch job using the updated batchPromptRequest method in BatchPromptService.
        String jobId = batchAiService.batchPromptRequest("review-generation", promptVariablesList, ids, AiReview.class);
        logger.info("Launched batch review generation job with ID: {}", jobId);
        // Write a tracking file (JSON) mapping the job ID to the list of product GTINs.
        File trackingFile = new File(trackingFolder, "tracking_" + jobId + ".json");
        Map<String, Object> trackingInfo = new HashMap<>();
        trackingInfo.put("jobId", jobId);
        trackingInfo.put("gtins", ids);
        try (FileWriter writer = new FileWriter(trackingFile, StandardCharsets.UTF_8)) {
            writer.write(objectMapper.writeValueAsString(trackingInfo));
        } catch (Exception e) {
            logger.error("Error writing tracking file for job {}: {}", jobId, e.getMessage());
        }
        return jobId;
    }

    /**
     * Scheduled method that scans the tracking folder for batch job tracking files.
     * <p>
     * For each tracking file, it reads the job ID and associated product GTINs,
     * checks the job status via batchPromptResponse (which will throw if not complete),
     * and if the job is complete, processes each BatchOutput to update the corresponding product review.
     * Finally, the tracking file is deleted.
     * TODO(p2,design) : separate the handle method
     * </p>
     */
    public void checkBatchJobStatuses() {
        File[] trackingFiles = trackingFolder.listFiles((dir, name) -> name.startsWith("tracking_") && name.endsWith(".json"));
        if (trackingFiles == null || trackingFiles.length == 0) {
            return;
        }
        for (File file : trackingFiles) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> trackingInfo = objectMapper.readValue(file, Map.class);
                String jobId = (String) trackingInfo.get("jobId");
                @SuppressWarnings("unchecked")
                // Retrieve batch results; this method throws if the job is not yet complete.
                PromptResponse<List<BatchOutput>> response = batchAiService.batchPromptResponse(jobId);
                if (response != null && response.getBody() != null && !response.getBody().isEmpty()) {
                    handleBatchResponse( jobId, response);
                 // Delete the tracking file after processing.
            		Files.deleteIfExists(file.toPath());
                }
            } catch (Exception e) {
                logger.error("Error processing tracking file {}: {}", file.getName(), e.getMessage(), e);
            }
        }
    }

    public void triggerResponseHandling(String jobId) throws ResourceNotFoundException, IOException {

    	 // Retrieve batch results; this method throws if the job is not yet complete.
        PromptResponse<List<BatchOutput>> response = batchAiService.batchPromptResponse(jobId);
        if (response != null && response.getBody() != null && !response.getBody().isEmpty()) {
            handleBatchResponse( jobId, response);
        }
    }


	private void handleBatchResponse( String jobId, PromptResponse<List<BatchOutput>> response) throws ResourceNotFoundException, IOException {
		logger.info("Batch job {} completed. Processing {} outputs.", jobId, response.getBody().size());
		// For each batch output (assumed to contain a customId that corresponds to the product GTIN)
		for (BatchOutput output : response.getBody()) {
		    String productGtin = output.customId();
		    // Retrieve the product from the repository.
		    Product product = productRepository.getById(Long.valueOf(productGtin));
		    if (product != null) {
		        // Process the BatchOutput to convert it into an AiReview.
		        AiReview newReview = processBatchOutputToAiReview(output);
		        if (null != newReview) {
		        	// Populate attributes
		        	populateAttributes(product, newReview);

		            AiReviewHolder holder = new AiReviewHolder();
		            holder.setCreatedMs(Instant.now().toEpochMilli());
		            holder.setReview(newReview);
		            holder.setEnoughData(true);
		            product.getReviews().put("fr", holder);
		            productRepository.forceIndex(product);
		            logger.info("Updated review for product with GTIN {}", productGtin);
		        } else {
		        	logger.error("Null AI review returned for {}", productGtin);
		        }
		    } else {
		        logger.warn("No product found with GTIN {}", productGtin);
		    }
		}

	}

    /**
     * Extract attrbutes from the gen ai response and populate the AiReview object
     * @param product
     * @param newReview
     */
	private void populateAttributes(Product product, AiReview newReview) {
		// Handling attributes
		newReview.getAttributes().stream().forEach(a -> {

			ProductAttribute agg = product.getAttributes().getAll().get(a.getName());
			if (null == agg) {
				// A first time match
				agg = new ProductAttribute();
				agg.setName(a.getName());
			}


			String source;
			try {
				// TODO : i18n, const or deduct  provider name from source
				// TODO : The add source behaviour should not be weared onfly. Means also shared code with

				source = "openai.com";
				agg.addSourceAttribute(new SourcedAttribute(new Attribute(a.getName(), a.getValue(), "fr") , source));

				// Replacing new AggAttribute in product
				product.getAttributes().getAll().put(agg.getName(), agg);
			} catch (Exception e1) {
				logger.error("Cannot extract domain name",e1);
			}



		});
	}

    // -------------------- Helper Methods -------------------- //
    /**
     * Extracts the domain from the URL.
     */
    private String extractDomain(String url) throws Exception {
        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain == null) {
            throw new Exception("Invalid URL: " + url);
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private AiReview processBatchOutputToAiReview(BatchOutput output) {
    	if (output.response().body().choices().size() > 1) {
    		logger.error("Error, multiple choices for {}", output);
    	}
    	//TODO(p2, perf) : instance variable
    	var outputConverter = new BeanOutputConverter<>(AiReview.class);

		  String jsonContent = output.response().body().choices().getFirst().message().getContent();
		  AiReview ret = outputConverter.convert(jsonContent);

		  ret = updateAiReviewReferences(ret);

//			AiReview ret = serialisationService.fromJson(jsonContent, AiReview.class);
		return ret;
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
        String description = replaceReferences(review.getDescription());
        String shortDescription = replaceReferences(review.getShortDescription());
        String mediumTitle = replaceReferences(review.getMediumTitle());
        String shortTitle = replaceReferences(review.getShortTitle());
        String technicalReview = replaceReferences(review.getTechnicalReview());
        String ecologicalReview = replaceReferences(review.getEcologicalReview());
        String summary = replaceReferences(review.getSummary());
        String dataQuality = replaceReferences(review.getDataQuality());
        List<String> pros = review.getPros().stream().map(this::replaceReferences).toList();
        List<String> cons = review.getCons().stream().map(this::replaceReferences).toList();
        List<AiReview.AiSource> sources = review.getSources().stream()
                .map(source -> new AiReview.AiSource(
                        source.getNumber(),
                        replaceReferences(source.getName()),
                        replaceReferences(source.getDescription()),
                        source.getUrl()
                ))
                .toList();
        List<AiReview.AiAttribute> attributes = review.getAttributes().stream()
                .map(attr -> new AiReview.AiAttribute(
                        replaceReferences(attr.getName()),
                        replaceReferences(attr.getValue()), attr.getNumber()
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
}
