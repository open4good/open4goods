package org.open4goods.services.reviewgeneration.service;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.stream.Stream;

import org.open4goods.model.ai.AiReview;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.review.ReviewGenerationStatus.Status;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.open4goods.services.prompt.dto.BatchResultItem;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.exceptions.BatchJobFailedException;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.prompt.service.provider.ProviderEvent;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;


/**
 * Service for generating AI-assisted reviews.
 * <p>
 * This implementation supports two review generation flows:
 * <ol>
 * <li>A realtime flow (using PromptService) that executes immediately.</li>
 * <li>A batch flow (using BatchPromptService) that leverages OpenAI’s JSONL
 * Batch API. In the batch flow, after submitting the job, a tracking file is
 * written with the job ID and product identifiers. A scheduled method later
 * scans the tracking folder; for each completed job, it retrieves the output
 * and updates the corresponding products.</li>
 * </ol>
 * <p>
 * In both cases, a common preprocessing stage is executed: search queries are
 * generated and executed via GoogleSearchService, URL content is fetched
 * concurrently via UrlFetchingService, markdown sources and token counts are
 * aggregated, and prompt variables are composed.
 * </p>
 *

 *
 */
@Service
public class ReviewGenerationService implements HealthIndicator {

	private static final Logger logger = LoggerFactory.getLogger(ReviewGenerationService.class);

	private final ReviewGenerationConfig properties;
	// Realtime prompt service for immediate generation.
	private final PromptService genAiService;
	// Batch prompt service for batch processing.
	private final BatchPromptService batchAiService;
	private final MeterRegistry meterRegistry;
	private final ProductRepository productRepository;
	private final ReviewGenerationPreprocessingService preprocessingService;
    private final VerticalsConfigService verticalsConfigService;

	// Thread pool executors.
	private final ThreadPoolExecutor executorService;
	// In-memory storage of process statuses (keyed by UPC).
	private final ConcurrentMap<Long, ReviewGenerationStatus> processStatusMap = new ConcurrentHashMap<>();
	// Global metrics.
	private final AtomicInteger totalProcessed = new AtomicInteger(0);
	private final AtomicInteger successfulCount = new AtomicInteger(0);
	private final AtomicInteger failedCount = new AtomicInteger(0);
	// Health flag.
	private volatile boolean lastGenerationFailed = false;

	// New fields for batch tracking.
	private final File trackingFolder;

	private final ObjectMapper objectMapper;
	private static final String AI_SOURCE_NAME = "AI";
	private static final String CITATIONS_METADATA_KEY = "citations";
    private static final int DEFAULT_SELECTION_MULTIPLIER = 5;

	private final BeanOutputConverter<AiReview> outputConverter;

	/**
	 * Constructs a new ReviewGenerationService.
	 *
	 * @param properties          the review generation configuration properties.
	 * @param googleSearchService the Google search service.
	 * @param urlFetchingService  the URL fetching service.
	 * @param genAiService        the realtime PromptService.
	 * @param batchAiService      the batch PromptService.
	 * @param meterRegistry       the actuator MeterRegistry.
	 * @param productRepository   the product repository service.
	 * @param preprocessingService the preprocessing service.
     * @param verticalsConfigService the verticals configuration service.
	 */
	public ReviewGenerationService(ReviewGenerationConfig properties, GoogleSearchService googleSearchService,
			UrlFetchingService urlFetchingService, PromptService genAiService, BatchPromptService batchAiService,
			MeterRegistry meterRegistry, ProductRepository productRepository, ReviewGenerationPreprocessingService preprocessingService,
            VerticalsConfigService verticalsConfigService) {
		this.properties = properties;
		this.genAiService = genAiService;
		this.batchAiService = batchAiService;
		this.meterRegistry = meterRegistry;
		this.productRepository = productRepository;
		this.preprocessingService = preprocessingService;
        this.verticalsConfigService = verticalsConfigService;
		this.executorService = new ThreadPoolExecutor(properties.getThreadPoolSize(), properties.getThreadPoolSize(),
				0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(properties.getMaxQueueSize()),
				new ThreadPoolExecutor.AbortPolicy());
		// Initialize the tracking folder based on configuration (or derive from batch
		// folder).
		String trackingPath = properties.getBatchFolder() + File.separator + "tracking";
		this.trackingFolder = new File(trackingPath);
		if (!this.trackingFolder.exists()) {
			this.trackingFolder.mkdirs();
		}
		this.objectMapper = new ObjectMapper();
		this.outputConverter = new BeanOutputConverter<>(AiReview.class);
	}

	// -------------------- Preprocessing Logic -------------------- //

	/**
	 * Checks if a review generation process for the given GTIN is already active.
	 *
	 * @param gtin the product GTIN.
	 * @return true if an active process exists; false otherwise.
	 */
	private boolean isActiveForGtin(String gtin) {
		return processStatusMap.values().stream().anyMatch(s -> s.getGtin() != null && s.getGtin().equals(gtin)
				&& s.getStatus() != Status.SUCCESS && s.getStatus() != Status.FAILED);
	}

	/**
	 * Determines if a new AI review should be generated for the product. Generation
	 * is required if the product does not already have a review or if the existing
	 * review is outdated.
	 *
	 * @param product the product to check.
	 * @return true if generation should proceed; false otherwise.
	 */
	private boolean shouldGenerateReview(Product product) {
		AiReviewHolder existingReview = product.getReviews().i18n("fr");
		if (existingReview == null || existingReview.getCreatedMs() == null) {
			return true;
		}

		// Check for validity of the existing review
		if (!isValidReview(existingReview.getReview())) {
			logger.warn("Existing AI review for UPC {} is invalid (insufficient content). Forcing regeneration.", product.getId());
			return true;
		}

		Instant reviewCreated = Instant.ofEpochMilli(existingReview.getCreatedMs());
		int delayDays = existingReview.isEnoughData() ? properties.getRegenerationDelayDays()
				: properties.getRetryDelayDays();
		Instant threshold = reviewCreated.plus(delayDays, ChronoUnit.DAYS);
		return Instant.now().isAfter(threshold);
	}

	/**
	 * Checks if an AI review is considered valid/complete enough to be kept.
	 * A valid review must not be null, must have a description of reasonable length
	 * (e.g. > 20 chars), and must have at least one attribute populated.
	 *
	 * @param review the review to check
	 * @return true if valid, false otherwise
	 */
	private boolean isValidReview(AiReview review) {
		if (review == null) {
			return false;
		}
		if (review.getDescription() == null || review.getDescription().trim().length() < 20) {
			return false;
		}
		if (review.getAttributes() == null || review.getAttributes().isEmpty()) {
			return false;
		}
		return true;
	}



	/**
	 * Asynchronous review generation using the realtime prompt service. (Uses a
	 * ThreadPoolExecutor to run the process asynchronously.)
	 *
	 * @param product             the product.
	 * @param verticalConfig      the vertical configuration.
	 * @param preProcessingFuture an optional external preprocessing future.
	 * @return the product UPC used to track generation status.
	 */
	public long generateReviewAsync(Product product, VerticalConfig verticalConfig,	CompletableFuture<Void> preProcessingFuture, boolean force) {
		return generateReviewAsync(product, verticalConfig, preProcessingFuture, force, new HashMap<>());
	}

	/**
	 * Asynchronous review generation using the realtime prompt service. (Uses a
	 * ThreadPoolExecutor to run the process asynchronously.)
	 *
	 * @param product             the product.
	 * @param verticalConfig      the vertical configuration.
	 * @param preProcessingFuture an optional external preprocessing future.
	 * @param force               force generation even if valid review exists
	 * @param customHeaders       custom headers (User-Agent, etc.) to use for fetching
	 * @return the product UPC used to track generation status.
	 */
	public long generateReviewAsync(Product product, VerticalConfig verticalConfig,	CompletableFuture<Void> preProcessingFuture, boolean force, Map<String, String> customHeaders) {
		long upc = product.getId();
		if (!force && !shouldGenerateReview(product)) {
			logger.info(
					"Skipping asynchronous AI review generation for UPC {} because an up-to-date review already exists.",
					upc);
			ReviewGenerationStatus status = new ReviewGenerationStatus();
			status.setUpc(upc);
			status.setGtin(product.gtin());
			status.setStatus(ReviewGenerationStatus.Status.ALREADY_PROCESSED);
			status.addMessage("Existing valid AI review found. Skipping generation.");
			status.setStartTime(Instant.now().toEpochMilli());
			status.setEndTime(Instant.now().toEpochMilli());
			computeTimings(status);
			processStatusMap.put(upc, status);
			return upc;
		}
		if (isActiveForGtin(product.gtin())) {
			throw new IllegalStateException(
					"Review generation already in progress for product with GTIN " + product.gtin());
		}
		processStatusMap.compute(upc, (key, existingStatus) -> {
			if (existingStatus != null && (existingStatus.getStatus() == ReviewGenerationStatus.Status.PENDING
					|| existingStatus.getStatus() == ReviewGenerationStatus.Status.QUEUED
					|| existingStatus.getStatus() == ReviewGenerationStatus.Status.SEARCHING
					|| existingStatus.getStatus() == ReviewGenerationStatus.Status.FETCHING
					|| existingStatus.getStatus() == ReviewGenerationStatus.Status.ANALYSING
					|| existingStatus.getStatus() == ReviewGenerationStatus.Status.PREPROCESSING)) {
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
				String promptKey = resolvePromptKey();
				PromptConfig promptConfig = genAiService.getPromptConfig(promptKey);
				if (promptConfig == null) {
					throw new ResourceNotFoundException("Prompt not found: " + promptKey);
				}
				Map<String, Object> promptVariables;
				if (promptConfig.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH) {
					status.setStatus(ReviewGenerationStatus.Status.SEARCHING);
					status.addMessage("AI grounding search...");
					status.addEvent(ReviewGenerationStatus.ProgressEventType.SEARCHING, "AI grounding search", null);
					promptVariables = preprocessingService.buildBasePromptVariables(product, verticalConfig);
				} else {
					status.setStatus(ReviewGenerationStatus.Status.SEARCHING);
					promptVariables = preprocessingService.preparePromptVariables(product, verticalConfig, status, customHeaders);
				}

				// Validation of critical variables
				List<String> requiredVars = List.of("PRODUCT_BRAND", "PRODUCT_MODEL", "VERTICAL_NAME", "OFFER_NAMES", "IMPACTSCORE_POSITION", "COMMON_ATTRIBUTES");
				for (String var : requiredVars) {
					if (!promptVariables.containsKey(var)) {
						throw new IllegalStateException("Missing required prompt variable: " + var);
					}
					// Also check for nulls if strict validation is required
					if (promptVariables.get(var) == null) {
						throw new IllegalStateException("Required prompt variable is null: " + var);
					}
				}

				status.addEvent(ReviewGenerationStatus.ProgressEventType.STARTED, "AI generation started", null);
				PromptResponse<AiReview> reviewResponse;
				if (promptConfig.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH) {
					reviewResponse = genAiService.objectPromptStream(promptKey, promptVariables, AiReview.class,
							event -> handleProviderEvent(status, event));
					logger.debug("Streaming review generation started for promptKey: {}", promptKey);

				} else {
					reviewResponse = genAiService.objectPrompt(promptKey, promptVariables, AiReview.class);
				}
				AiReview newReview = processAiReview(reviewResponse.getBody(), reviewResponse.getMetadata());

				// Populate attributes and resources
				populateAttributes(product, newReview, reviewResponse.getMetadata());
				addResources(product, newReview);
				logger.info("Completed review for UPC {}: {}", upc, objectMapper.writeValueAsString(newReview));
				holder.setReview(newReview);

				// Safely extract token info
				@SuppressWarnings("unchecked")
				Map<String, Integer> sourceTokens = (Map<String, Integer>) promptVariables.getOrDefault("SOURCE_TOKENS", new HashMap<>());
				holder.setSources(sourceTokens);
				holder.setTotalTokens((Integer) promptVariables.getOrDefault("TOTAL_TOKENS", 0));

				holder.setEnoughData(true);
				status.setResult(holder);
				status.setStatus(ReviewGenerationStatus.Status.SUCCESS);
				status.addEvent(ReviewGenerationStatus.ProgressEventType.COMPLETED, "AI generation completed", null);
				meterRegistry.counter("review.generation.success").increment();
			} catch (NotEnoughDataException e) {
				logger.warn("Not enought data for generating ai review for {}", product);
				holder.setEnoughData(false);
				status.setResult(holder);
				status.setStatus(ReviewGenerationStatus.Status.FAILED);
				status.setErrorMessage("Not enough data to generate an AI review for this product.");
				status.addEvent(ReviewGenerationStatus.ProgressEventType.ERROR, status.getErrorMessage(), null);
				meterRegistry.counter("review.generation.failed").increment();
				lastGenerationFailed = true;
				lastGenerationFailed = true;
			} catch (Throwable e) {
                e.printStackTrace();
				logger.error("Asynchronous review generation failed for UPC {}: {}", upc, e.getMessage(), e);
				status.setStatus(ReviewGenerationStatus.Status.FAILED);
				status.setErrorMessage(e.getMessage());
				status.addEvent(ReviewGenerationStatus.ProgressEventType.ERROR, status.getErrorMessage(), null);
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
     * Scheduled batch trigger that runs every morning at 6 AM.
     */
    @Scheduled(cron = "${review.generation.batch-schedule-cron:0 0 6 * * *}")
    public void scheduleDailyBatchReviewGeneration()
    {
        triggerNextTopImpactScoreBatches(properties.getBatchScheduleSize(), true);
    }

    /**
     * Trigger next-top impact score batch generation for all enabled verticals.
     *
     * @param limit number of products to include per vertical
     * @return list of batch job identifiers
     */
    public List<String> triggerNextTopImpactScoreBatches(int limit, boolean sortOnImpactScore)
    {
        int effectiveLimit = Math.max(limit, 1);
        List<String> jobIds = new ArrayList<>();
        for (VerticalConfig verticalConfig : verticalsConfigService.getConfigsWithoutDefault(true)) {
            try {
                String jobId = generateNextTopImpactScoreBatch(verticalConfig, effectiveLimit, sortOnImpactScore);
                if (!"NoBatchJob".equals(jobId)) {
                    jobIds.add(jobId);
                }
            } catch (Exception e) {
                lastGenerationFailed = true;
                logger.error("Failed to schedule batch review generation for vertical {}: {}",
                        verticalConfig.getId(), e.getMessage(), e);
            }
        }
        return jobIds;
    }

    /**
     * Trigger next-top impact score batch generation for a specific vertical.
     *
     * @param verticalId vertical identifier
     * @param limit number of products to include
     * @return batch job identifier
     * @throws IOException when batch submission fails
     */
    public String triggerNextTopImpactScoreBatch(String verticalId, int limit, boolean sortOnImpactScore) throws IOException
    {
        Objects.requireNonNull(verticalId, "verticalId is required");
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(verticalId);
        return generateNextTopImpactScoreBatch(verticalConfig, limit, sortOnImpactScore);
    }

    /**
     * Build and submit a batch job for the next top impact score products.
     *
     * @param verticalConfig vertical configuration
     * @param limit number of products to include
     * @return batch job identifier
     * @throws IOException when batch submission fails
     */
    public String generateNextTopImpactScoreBatch(VerticalConfig verticalConfig, int limit, boolean sortOnImpactScore) throws IOException
    {
        Objects.requireNonNull(verticalConfig, "verticalConfig is required");
        int effectiveLimit = Math.max(limit, 1);
        List<Product> products = loadNextTopImpactScoreProducts(verticalConfig, effectiveLimit, sortOnImpactScore);
        if (products.isEmpty()) {
            logger.warn("No eligible products found for impact score batch in vertical {}", verticalConfig.getId());
            return "NoBatchJob";
        }
        return generateReviewBatchRequest(products, verticalConfig);
    }

    private List<Product> loadNextTopImpactScoreProducts(VerticalConfig verticalConfig, int limit, boolean sortOnImpactScore)
    {
        int selectionLimit = Math.max(limit * DEFAULT_SELECTION_MULTIPLIER, limit);

        try (Stream<Product> productStream =
                productRepository.exportVerticalWithValidDateAndMissingReviewOrderByImpactScore(verticalConfig.getId(), "fr", selectionLimit, false, sortOnImpactScore)) {
            return productStream
                    .filter(product -> !isActiveForBatch(product))
                    .limit(limit)
                    .toList();
        }
    }


    private boolean isActiveForBatch(Product product)
    {
        if (product == null) {
            return false;
        }
        ReviewGenerationStatus status = processStatusMap.get(product.getId());
        if (status != null && status.getStatus() != Status.SUCCESS && status.getStatus() != Status.FAILED) {
            return true;
        }
        return isActiveForGtin(product.gtin());
    }


	/**
	 * Adds resources (images, PDFs, videos) from the AI review to the product.
	 * Only valid and reachable resources are added.
	 *
	 * @param product the product to update
	 * @param newReview the AI generated review containing potential resources
	 */
	private void addResources(Product product, AiReview newReview) {
		if (newReview == null) return;

		logger.info("Processing resources for product {}", product.getId());

		List<String> validImages = filterValidUrls(newReview.getImages(), "Image");
		validImages.forEach(image -> safeAddResource(product, image));

		List<String> validPdfs = filterValidUrls(newReview.getPdfs(), "PDF");
		validPdfs.forEach(pdf -> safeAddResource(product, pdf));

		List<String> validVideos = filterValidUrls(newReview.getVideos(), "Video");
		validVideos.forEach(video -> safeAddResource(product, video));
	}

	/**
	 * Safely adds a resource url to the product, catching validation exceptions.
	 *
	 * @param product the product
	 * @param url the resource url
	 */
	private void safeAddResource(Product product, String url) {
		try {
			product.addResource(new Resource(url));
			logger.debug("Added resource {} to product {}", url, product.getId());
		} catch (ValidationException e) {
			logger.warn("Error while validating LLM returned resource: {}", url);
		}
	}

	/**
	 * Filters a list of URLs, retaining only those that are valid and reachable.
	 *
	 * @param urls list of URLs to check
	 * @param typeStr description of the resource type for logging
	 * @return list of valid URLs
	 */
	private List<String> filterValidUrls(List<String> urls, String typeStr) {
		if (urls == null) return List.of();
		return urls.stream().filter(url -> {
			boolean valid = isValidUrl(url);
			if (!valid) {
				logger.warn("{} URL ignored (invalid or unreachable): {}", typeStr, url);
			}
			return valid;
		}).toList();
	}

	private boolean isValidUrl(String urlString) {
		if (urlString == null || urlString.isBlank()) {
			return false;
		}
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("HEAD");
			connection.setConnectTimeout(3000);
			connection.setReadTimeout(3000);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			int status = connection.getResponseCode();
			return status >= 200 && status < 400;
		} catch (Exception e) {
			logger.warn("URL validation failed for {}: {}", urlString, e.getMessage());
			return false;
		}
	}

	/**
	 * Submits a batch review generation request.
	 * <p>
	 * For each eligible product, it prepares prompt variables (including Google
	 * searches, URL fetching, token counting, etc.) and collects the product GTINs.
	 * Then it submits a batch job via the BatchPromptService using the prompt key
	 * "review-generation". Instead of using a future callback, a tracking file is
	 * written to a designated folder so that a scheduled task can later check job
	 * status and process the results. Tracking metadata persists product IDs and
	 * GTINs for traceability.
	 * </p>
	 *
	 * @param products       the list of products to process.
	 * @param verticalConfig the vertical configuration.
	 * @return the batch job ID.
	 * @throws IOException
	 */
	public String generateReviewBatchRequest(List<Product> products, VerticalConfig verticalConfig) throws IOException {
		List<Map<String, Object>> promptVariablesList = new ArrayList<>();
		List<String> productIds = new ArrayList<>();
		List<String> gtins = new ArrayList<>();
		String promptKey = resolvePromptKey();
		PromptConfig promptConfig = genAiService.getPromptConfig(promptKey);
		if (promptConfig != null && promptConfig.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH) {
			logger.info("Batch review generation with model-native search enabled.");
		}
		for (Product product : products) {
			long upc = product.getId();
			if (!shouldGenerateReview(product)) {
				logger.info("Skipping AI review generation for UPC {} because an up-to-date review already exists.",
						upc);
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
				if (promptConfig != null && promptConfig.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH) {
					promptVariablesList.add(preprocessingService.buildBasePromptVariables(product, verticalConfig));
				} else {
					promptVariablesList.add(preprocessingService.preparePromptVariables(product, verticalConfig, status));
				}
				productIds.add(String.valueOf(product.getId()));
				gtins.add(product.gtin());
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
		// Submit batch job using the updated batchPromptRequest method in
		// BatchPromptService.
		String jobId = batchAiService.batchPromptRequest(promptKey, promptVariablesList, productIds, AiReview.class);
		logger.info("Launched batch review generation job with ID: {}", jobId);
		// Write a tracking file (JSON) mapping the job ID to the list of product IDs.
		File trackingFile = new File(trackingFolder, "tracking_" + jobId + ".json");
		Map<String, Object> trackingInfo = new HashMap<>();
		trackingInfo.put("jobId", jobId);
		trackingInfo.put("productIds", productIds);
		trackingInfo.put("gtins", gtins);
		trackingInfo.put("verticalId", verticalConfig.getId());
		trackingInfo.put("createdAt", Instant.now().toEpochMilli());
		try (FileWriter writer = new FileWriter(trackingFile, StandardCharsets.UTF_8)) {
			writer.write(objectMapper.writeValueAsString(trackingInfo));
		} catch (Exception e) {
			logger.error("Error writing tracking file for job {}: {}", jobId, e.getMessage());
		}
		return jobId;
	}

	private String resolvePromptKey() {
		if (properties.isUseGroundedPrompt()) {
			return properties.getGroundedPromptKey();
		}
		return properties.getPromptKey();
	}

	/**
	 * Scheduled method that scans the tracking folder for batch job tracking files.
	 * <p>
	 * For each tracking file, it reads the job ID and associated product GTINs,
	 * checks the job status via batchPromptResponse (which will throw if not
	 * complete), and if the job is complete, processes each batch result to update
	 * the corresponding product review. Finally, the tracking file is deleted.
	 * </p>
	 */
	@Scheduled(fixedDelayString = "${review.generation.batch-poll-interval:PT5M}")
	public void checkBatchJobStatuses() {
		logger.info("Executing scheduled batch job status check...");
		File[] trackingFiles = trackingFolder
				.listFiles((dir, name) -> name.startsWith("tracking_") && name.endsWith(".json"));
		if (trackingFiles == null || trackingFiles.length == 0) {
			return;
		}
		for (File file : trackingFiles) {
			handleTrackingFile(file);
		}
	}

	/**
	 * Processes a single tracking file.
	 *
	 * @param file the tracking file to process.
	 */
	private void handleTrackingFile(File file) {
        Map<String, Object> trackingInfo = null;
        String jobId = null;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> parsedInfo = objectMapper.readValue(file, Map.class);
            trackingInfo = parsedInfo;
			jobId = (String) trackingInfo.get("jobId");
			// Retrieve batch results; this method throws if the job is not yet complete.
			PromptResponse<List<BatchResultItem>> response = batchAiService.batchPromptResponse(jobId);
			if (response != null && response.getBody() != null && !response.getBody().isEmpty()) {
				handleBatchResponse(jobId, response);
				// Delete the tracking file after processing.
				Files.deleteIfExists(file.toPath());
			}
		} catch (BatchJobFailedException e) {
            if (trackingInfo != null && jobId != null) {
                handleBatchFailure(jobId, trackingInfo, e.getMessage());
                deleteTrackingFile(file, jobId);
            }
            logger.error("Batch job {} failed for tracking file {}: {}", jobId, file.getName(), e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error processing tracking file {}: {}", file.getName(), e.getMessage(), e);
		}
	}

    private void handleBatchFailure(String jobId, Map<String, Object> trackingInfo, String errorMessage)
    {
        Object productIds = trackingInfo.get("productIds");
        if (productIds instanceof List<?> list) {
            for (Object productId : list) {
                if (productId == null) {
                    continue;
                }
                try {
                    updateBatchStatus(Long.parseLong(productId.toString()), ReviewGenerationStatus.Status.FAILED, errorMessage);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid product id in tracking file for job {}: {}", jobId, productId);
                }
            }
        }
        lastGenerationFailed = true;
    }

    private void deleteTrackingFile(File file, String jobId)
    {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            logger.warn("Failed to delete tracking file for job {}: {}", jobId, e.getMessage());
        }
    }

	public void triggerResponseHandling(String jobId) throws ResourceNotFoundException, IOException {

		// Retrieve batch results; this method throws if the job is not yet complete.
		PromptResponse<List<BatchResultItem>> response = batchAiService.batchPromptResponse(jobId);
		if (response != null && response.getBody() != null && !response.getBody().isEmpty()) {
			handleBatchResponse(jobId, response);
		}
	}

	private void handleBatchResponse(String jobId, PromptResponse<List<BatchResultItem>> response)
			throws ResourceNotFoundException, IOException {
		logger.info("Batch job {} completed. Processing {} outputs.", jobId, response.getBody().size());
		// For each batch output (assumed to contain a customId that corresponds to the
		// product ID)
		for (BatchResultItem output : response.getBody()) {
			String productId = output.customId();
			// Retrieve the product from the repository.
			Product product = productRepository.getById(Long.valueOf(productId));
			if (product != null) {
				// Process the batch result to convert it into an AiReview.
				AiReview newReview = processAiReview(output);
				if (null != newReview) {
					// Populate attributes
					populateAttributes(product, newReview, response.getMetadata());

					AiReviewHolder holder = new AiReviewHolder();
					holder.setCreatedMs(Instant.now().toEpochMilli());
					holder.setReview(newReview);
					holder.setEnoughData(true);
					product.getReviews().put("fr", holder);
					productRepository.forceIndex(product);
					updateBatchStatus(product.getId(), ReviewGenerationStatus.Status.SUCCESS, null);
					logger.info("Updated review for product with ID {}", productId);
				} else {
					updateBatchStatus(product.getId(), ReviewGenerationStatus.Status.FAILED,
							"Null AI review returned from batch response.");
					logger.error("Null AI review returned for {}", productId);
				}
			} else {
				updateBatchStatus(Long.valueOf(productId), ReviewGenerationStatus.Status.FAILED,
						"No product found for batch response.");
				logger.warn("No product found with ID {}", productId);
			}
		}

	}

	private void updateBatchStatus(long productId, ReviewGenerationStatus.Status status, String errorMessage) {
		ReviewGenerationStatus currentStatus = processStatusMap.compute(productId, (key, existingStatus) -> {
			if (existingStatus == null) {
				ReviewGenerationStatus newStatus = new ReviewGenerationStatus();
				newStatus.setUpc(productId);
				newStatus.setStatus(status);
				newStatus.setStartTime(Instant.now().toEpochMilli());
				return newStatus;
			}
			return existingStatus;
		});
		currentStatus.setStatus(status);
		if (errorMessage != null) {
			currentStatus.setErrorMessage(errorMessage);
			currentStatus.addEvent(ReviewGenerationStatus.ProgressEventType.ERROR, errorMessage, null);
		} else if (status == ReviewGenerationStatus.Status.SUCCESS) {
			currentStatus.addEvent(ReviewGenerationStatus.ProgressEventType.COMPLETED,
					"Batch review generation completed", null);
		}
		currentStatus.setEndTime(Instant.now().toEpochMilli());
		computeTimings(currentStatus);
		if (status == ReviewGenerationStatus.Status.SUCCESS) {
			successfulCount.incrementAndGet();
			totalProcessed.incrementAndGet();
		} else if (status == ReviewGenerationStatus.Status.FAILED) {
			failedCount.incrementAndGet();
			totalProcessed.incrementAndGet();
			lastGenerationFailed = true;
		}
	}

	/**
	 * Extract attributes from the gen ai response and populate the AiReview object.
	 *
	 * @param product the product to update
	 * @param review the new AI review
	 */
	private void populateAttributes(Product product, AiReview review, Map<String, Object> metadata) {
		// Handling attributes
		String providerName = determineProviderName(metadata);

		review.getAttributes().stream().forEach(a -> {

			ProductAttribute agg = product.getAttributes().getAll().get(a.getName());
			if (null == agg) {
				// A first time match
				agg = new ProductAttribute();
				agg.setName(a.getName());
			}

			try {
				agg.addSourceAttribute(new SourcedAttribute(new Attribute(a.getName(), a.getValue(), "fr"), providerName));

				// Replacing new AggAttribute in product
				product.getAttributes().getAll().put(agg.getName(), agg);
			} catch (Exception e1) {
				logger.error("Cannot extract domain name", e1);
			}

		});
	}

	private String determineProviderName(Map<String, Object> metadata) {
		if (metadata != null) {
			Object provider = metadata.get("provider");
			if (provider != null) {
				return provider.toString();
			}
			Object model = metadata.get("model");
			if (model != null) {
				return model.toString();
			}
		}
		return AI_SOURCE_NAME;
	}

	// -------------------- Helper Methods -------------------- //


	private AiReview processAiReview(BatchResultItem output) {
		String jsonContent = output.content();
        if (jsonContent == null || jsonContent.isBlank()) {
            logger.warn("Empty content for customId {}. Raw line: {}", output.customId(), output.raw());
            return null;
        }
        String sanitizedContent = jsonContent.replace("```json", "").replace("```", "").trim();
        logger.debug("Processing content for customId {}: {}", output.customId(), sanitizedContent);
		AiReview ret = outputConverter.convert(sanitizedContent);
		return processAiReview(ret, null);
	}

	/**
	 * Consolidates post-processing of an AI review: normalization, references, and URL resolution.
	 *
	 * @param review   the AI review to process
	 * @param metadata optional metadata from the AI provider
	 * @return the processed AI review
	 */
	private AiReview processAiReview(AiReview review, Map<String, Object> metadata) {
		if (review == null) return null;

		// 1. Apply citations from metadata if available (for grounded prompts)
		AiReview processed = applyCitations(review, metadata);

		// 2. Normalize text (e.g. em dashes)
		processed = normalizeReviewText(processed);

		// 3. Replace numeric references with HTML links
		processed = updateAiReviewReferences(processed);

		// 4. Resolve URLs (redirects)
		processed = postProcess30x(processed);

		return processed;
	}

	private AiReview applyCitations(AiReview review, Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return review;
		}
		List<AiReview.AiSource> metadataSources = resolveSourcesFromMetadata(metadata);
		if (metadataSources.isEmpty()) {
			return review;
		}

		int maxSourceNumber = metadataSources.size();
		ReferenceNormalization normalization = normalizeReferences(review, metadataSources, maxSourceNumber);

		return copyWithNewData(review, normalization, metadataSources);
	}

	private AiReview copyWithNewData(AiReview original, ReferenceNormalization normalization, List<AiReview.AiSource> sources) {
		return new AiReview(
				normalization.description(),
				normalization.technicalOneline(),
				normalization.technicalShortReview(),
				normalization.ecologicalOneline(),
				normalization.communityOneline(),
				normalization.shortDescription(),
				normalization.mediumTitle(),
				normalization.shortTitle(),
				original.getBaseLine(),
				original.getManufacturingCountry(),
				normalization.technicalReviewNovice(),
				normalization.technicalReviewIntermediate(),
				normalization.technicalReviewAdvanced(),
				normalization.ecologicalReviewNovice(),
				normalization.ecologicalReviewIntermediate(),
				normalization.ecologicalReviewAdvanced(),
				normalization.communityReviewNovice(),
				normalization.communityReviewIntermediate(),
				normalization.communityReviewAdvanced(),
				normalization.summary(),
				normalization.pros(),
				normalization.cons(),
				sources,
				normalization.attributes(),
				normalization.dataQuality(),
				original.getRatings(),
				normalization.pdfs(),
				normalization.images(),
				normalization.videos(),
				normalization.socialLinks()
		);
	}

	/**
	 * Normalizes text content in the review (e.g. replacing em dashes).
	 *
	 * @param review the review to normalize
	 * @return the normalized review
	 */
	private AiReview normalizeReviewText(AiReview review) {
		if (review == null) return null;

		ReferenceNormalization normalization = new ReferenceNormalization(
				normalizeText(review.getDescription()),
				normalizeText(review.getShortDescription()),
				normalizeText(review.getMediumTitle()),
				normalizeText(review.getShortTitle()),
				normalizeText(review.getSummary()),
				review.getPros() == null ? List.of() : review.getPros().stream().map(this::normalizeText).toList(),
				review.getCons() == null ? List.of() : review.getCons().stream().map(this::normalizeText).toList(),
				review.getAttributes() == null ? List.of() : review.getAttributes().stream()
						.map(a -> new AiReview.AiAttribute(normalizeText(a.getName()), normalizeText(a.getValue()), a.getNumber()))
						.toList(),
				normalizeText(review.getDataQuality()),
				normalizeText(review.getTechnicalOneline()),
				normalizeText(review.getTechnicalShortReview()),
				normalizeText(review.getEcologicalOneline()),
				normalizeText(review.getCommunityOneline()),
				resolveUrlList(review.getPdfs()),
				resolveUrlList(review.getImages()),
				resolveUrlList(review.getVideos()),
				resolveUrlList(review.getSocialLinks()),
				normalizeText(review.getTechnicalReviewNovice()),
				normalizeText(review.getTechnicalReviewIntermediate()),
				normalizeText(review.getTechnicalReviewAdvanced()),
				normalizeText(review.getEcologicalReviewNovice()),
				normalizeText(review.getEcologicalReviewIntermediate()),
				normalizeText(review.getEcologicalReviewAdvanced()),
				normalizeText(review.getCommunityReviewNovice()),
				normalizeText(review.getCommunityReviewIntermediate()),
				normalizeText(review.getCommunityReviewAdvanced())
		);

		List<AiReview.AiSource> normalizedSources = review.getSources() == null ? List.of() : review.getSources().stream()
				.map(s -> new AiReview.AiSource(s.getNumber(), normalizeText(s.getName()), normalizeText(s.getDescription()), s.getUrl()))
				.toList();

		return copyWithNewData(review, normalization, normalizedSources);
	}

	private String normalizeText(String text) {
		if (text == null) return null;
		// Replace em dash (—) with hyphen (-)
		return text.replace("—", "-").trim();
	}

	/**
	 * Post-process the review, e.g. resolving URLs.
	 *
	 * @param review the review to process
	 * @return the processed review
	 */
	private AiReview postProcess30x(AiReview review) {
		if (!properties.isResolveUrl() || review == null) {
			return review;
		}
		List<AiReview.AiSource> updatedSources = review.getSources() == null ? List.of() : review.getSources().stream().map(source -> {
			String originalUrl = source.getUrl();
			String resolvedUrl = resolveUrl(originalUrl);

			if (isExcludedDomain(resolvedUrl)) {
				logger.info("URL {} removed because it matches an excluded domain.", resolvedUrl);
				return null;
			}
			if (!isValidUrl(resolvedUrl)) {
				logger.warn("URL {} removed because it is invalid or unreachable.", resolvedUrl);
				return null;
			}

			if (originalUrl != null && !originalUrl.equals(resolvedUrl)) {
				return new AiReview.AiSource(source.getNumber(), source.getName(), source.getDescription(), resolvedUrl);
			}
			return source;
		})
		.filter(Objects::nonNull)
		.toList();

		ReferenceNormalization normalization = new ReferenceNormalization(
				review.getDescription(),
				review.getShortDescription(),
				review.getMediumTitle(),
				review.getShortTitle(),
				review.getSummary(),
				review.getPros(),
				review.getCons(),
				review.getAttributes(),
				review.getDataQuality(),
				review.getTechnicalOneline(),
				review.getTechnicalShortReview(),
				review.getEcologicalOneline(),
				review.getCommunityOneline(),
				resolveUrlList(review.getPdfs()),
				resolveUrlList(review.getImages()),
				resolveUrlList(review.getVideos()),
				resolveUrlList(review.getSocialLinks()),
				review.getTechnicalReviewNovice(),
				review.getTechnicalReviewIntermediate(),
				review.getTechnicalReviewAdvanced(),
				review.getEcologicalReviewNovice(),
				review.getEcologicalReviewIntermediate(),
				review.getEcologicalReviewAdvanced(),
				review.getCommunityReviewNovice(),
				review.getCommunityReviewIntermediate(),
				review.getCommunityReviewAdvanced()
		);

		return copyWithNewData(review, normalization, updatedSources);
	}

	private List<String> resolveUrlList(List<String> urls) {
		if (urls == null || urls.isEmpty()) {
			return List.of();
		}
		return urls.stream().map(this::resolveUrl).toList();
	}

	/**
	 * Resolves a URL by following redirects (up to a limit).
	 *
	 * @param urlString the URL to resolve
	 * @return the final URL after following redirects, or the original if no redirect or error
	 */
	private String resolveUrl(String urlString) {
		if (urlString == null || urlString.isBlank()) {
			return urlString;
		}

		String currentUrl = urlString;
		int maxRedirects = 5;
		int redirects = 0;

		try {
			while (redirects < maxRedirects) {
				URL url = URI.create(currentUrl).toURL();
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setInstanceFollowRedirects(false); // Handle redirects manually
				connection.setRequestMethod("HEAD");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);

				// User Agent to avoid being blocked by some servers
				connection.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

				int status = connection.getResponseCode();

				if (status >= 300 && status < 400) {
					String location = connection.getHeaderField("Location");
					if (location != null && !location.isBlank()) {
						// Handle relative URLs
						URI baseUri = url.toURI();
						URI resolvedUri = baseUri.resolve(location);
						String nextUrl = resolvedUri.toString();

						logger.debug("Redirecting {} -> {}", currentUrl, nextUrl);
						currentUrl = nextUrl;
						redirects++;
					} else {
						// Redirect with no location?
						logger.warn("Redirect response for {} but no Location header found.", currentUrl);
						break;
					}
				} else {
					// Not a redirect (2xx, 4xx, 5xx), stop here
					break;
				}
			}

			if (redirects >= maxRedirects) {
				logger.warn("Too many redirects for URL: {}", urlString);
			}

			return currentUrl;

		} catch (Exception e) {
			logger.warn("Failed to resolve URL {}: {}", urlString, e.getMessage());
			return urlString; // Return original if failure
		}
	}

	/**
	 * Computes and sets the duration and remaining time for a process based on
	 * start and end times.
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
		if (status.getStartTime() != null && (status.getStatus() != ReviewGenerationStatus.Status.SUCCESS
				&& status.getStatus() != ReviewGenerationStatus.Status.FAILED)) {
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
		if (status != null && status.getStatus() != ReviewGenerationStatus.Status.SUCCESS
				&& status.getStatus() != ReviewGenerationStatus.Status.FAILED) {
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
				.filter(s -> s.getStatus() == ReviewGenerationStatus.Status.PENDING
						|| s.getStatus() == ReviewGenerationStatus.Status.QUEUED
						|| s.getStatus() == ReviewGenerationStatus.Status.SEARCHING
						|| s.getStatus() == ReviewGenerationStatus.Status.FETCHING
						|| s.getStatus() == ReviewGenerationStatus.Status.ANALYSING)
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
	 * Updates textual fields in the provided AiReview by replacing numeric
	 * references with HTML links.
	 *
	 * @param review the original AiReview.
	 * @return a new AiReview with updated text fields.
	 */
	private AiReview updateAiReviewReferences(AiReview review) {
		if (review == null) return null;

		ReferenceNormalization normalization = new ReferenceNormalization(
				replaceReferences(review.getDescription()),
				replaceReferences(review.getShortDescription()),
				replaceReferences(review.getMediumTitle()),
				replaceReferences(review.getShortTitle()),
				replaceReferences(review.getSummary()),
				review.getPros() == null ? List.of() : review.getPros().stream().map(this::replaceReferences).toList(),
				review.getCons() == null ? List.of() : review.getCons().stream().map(this::replaceReferences).toList(),
				review.getAttributes() == null ? List.of() : review.getAttributes().stream()
						.map(attr -> new AiReview.AiAttribute(replaceReferences(attr.getName()),
								replaceReferences(attr.getValue()), attr.getNumber()))
						.toList(),
				replaceReferences(review.getDataQuality()),
				replaceReferences(review.getTechnicalOneline()),
				replaceReferences(review.getTechnicalShortReview()),
				replaceReferences(review.getEcologicalOneline()),
				replaceReferences(review.getCommunityOneline()),
				review.getPdfs(),
				review.getImages(),
				review.getVideos(),
				review.getSocialLinks(),
				replaceReferences(review.getTechnicalReviewNovice()),
				replaceReferences(review.getTechnicalReviewIntermediate()),
				replaceReferences(review.getTechnicalReviewAdvanced()),
				replaceReferences(review.getEcologicalReviewNovice()),
				replaceReferences(review.getEcologicalReviewIntermediate()),
				replaceReferences(review.getEcologicalReviewAdvanced()),
				replaceReferences(review.getCommunityReviewNovice()),
				replaceReferences(review.getCommunityReviewIntermediate()),
				replaceReferences(review.getCommunityReviewAdvanced())
		);

		List<AiReview.AiSource> sources = review.getSources() == null ? List.of() : review.getSources().stream()
				.map(source -> new AiReview.AiSource(source.getNumber(), replaceReferences(source.getName()),
						replaceReferences(source.getDescription()), source.getUrl()))
				.toList();

		return copyWithNewData(review, normalization, sources);
	}

	@Override
	public Health health() {
		if (lastGenerationFailed) {
			return Health.down().withDetail("error", "One or more review generations have failed").build();
		}
		return Health.up().build();
	}

	private void handleProviderEvent(ReviewGenerationStatus status, ProviderEvent event) {
		if (event == null) {
			return;
		}
		switch (event.getType()) {
			case STARTED -> status.addEvent(ReviewGenerationStatus.ProgressEventType.STARTED,
					"Streaming generation started", null);
			case SEARCHING -> status.addEvent(ReviewGenerationStatus.ProgressEventType.SEARCHING,
					event.getContent(), null);
			case STREAM_CHUNK -> status.addEvent(ReviewGenerationStatus.ProgressEventType.STREAM_CHUNK,
					null, event.getContent());
			case COMPLETED -> status.addEvent(ReviewGenerationStatus.ProgressEventType.COMPLETED,
					"Streaming generation completed", null);
			case ERROR -> status.addEvent(ReviewGenerationStatus.ProgressEventType.ERROR,
					event.getErrorMessage(), null);
			default -> {
			}
		}
	}

	private AiReview applyCitationsAndNormalize(AiReview review, Map<String, Object> metadata) {
		return processAiReview(review, metadata);
	}

	private List<AiReview.AiSource> resolveSourcesFromMetadata(Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return List.of();
		}
		Object citations = metadata.get(CITATIONS_METADATA_KEY);
		if (!(citations instanceof List<?> citationList)) {
			return List.of();
		}
		List<AiReview.AiSource> sources = new ArrayList<>();
		int index = 1;
		for (Object citation : citationList) {
			if (!(citation instanceof Map<?, ?> citationMap)) {
				continue;
			}
			String url = citationMap.get("url") != null ? citationMap.get("url").toString() : null;
			if (url == null || url.isBlank()) {
				continue;
			}
			String title = citationMap.get("title") != null ? citationMap.get("title").toString() : url;
			String snippet = citationMap.get("snippet") != null ? citationMap.get("snippet").toString() : "";
			sources.add(new AiReview.AiSource(index++, title, snippet, url));
		}
		return sources;
	}

	private ReferenceNormalization normalizeReferences(AiReview review, List<AiReview.AiSource> sources,
			int maxSourceNumber) {
		ReferenceNormalizer normalizer = new ReferenceNormalizer(maxSourceNumber);
		String description = normalizer.normalize(review.getDescription());
		String shortDescription = normalizer.normalize(review.getShortDescription());
		String mediumTitle = normalizer.normalize(review.getMediumTitle());
		String shortTitle = normalizer.normalize(review.getShortTitle());
		String technicalReviewNovice = normalizer.normalize(review.getTechnicalReviewNovice());
		String technicalReviewIntermediate = normalizer.normalize(review.getTechnicalReviewIntermediate());
		String technicalReviewAdvanced = normalizer.normalize(review.getTechnicalReviewAdvanced());

		String ecologicalReviewNovice = normalizer.normalize(review.getEcologicalReviewNovice());
		String ecologicalReviewIntermediate = normalizer.normalize(review.getEcologicalReviewIntermediate());
		String ecologicalReviewAdvanced = normalizer.normalize(review.getEcologicalReviewAdvanced());

		String communityReviewNovice = normalizer.normalize(review.getCommunityReviewNovice());
		String communityReviewIntermediate = normalizer.normalize(review.getCommunityReviewIntermediate());

		String communityReviewAdvanced = normalizer.normalize(review.getCommunityReviewAdvanced());

		String summary = normalizer.normalize(review.getSummary());
		String technicalOneline = normalizer.normalize(review.getTechnicalOneline());
		String technicalShortReview = normalizer.normalize(review.getTechnicalShortReview());
		String ecologicalOneline = normalizer.normalize(review.getEcologicalOneline());
		String communityOneline = normalizer.normalize(review.getCommunityOneline());
		String dataQuality = normalizer.normalize(review.getDataQuality());

		List<String> pros = review.getPros() == null ? List.of() : review.getPros().stream().map(normalizer::normalize).toList();
		List<String> cons = review.getCons() == null ? List.of() : review.getCons().stream().map(normalizer::normalize).toList();
		List<AiReview.AiAttribute> attributes = review.getAttributes() == null ? List.of() : review.getAttributes().stream()
				.map(a -> new AiReview.AiAttribute(normalizer.normalize(a.getName()), normalizer.normalize(a.getValue()), a.getNumber()))
				.toList();
		if (maxSourceNumber == 0) {
			dataQuality = appendDataQuality(dataQuality, "Aucune source fiable n'a été trouvée.");
		} else if (normalizer.hasRemovedReferences()) {
			dataQuality = appendDataQuality(dataQuality,
					"Certaines références ont été retirées car elles ne correspondaient à aucune source.");
		}
		return new ReferenceNormalization(description, shortDescription, mediumTitle, shortTitle,
				summary, pros, cons, attributes, dataQuality, technicalOneline, technicalShortReview,
				ecologicalOneline, communityOneline, review.getPdfs(), review.getImages(), review.getVideos(),
				review.getSocialLinks(),
				technicalReviewNovice, technicalReviewIntermediate, technicalReviewAdvanced,
				ecologicalReviewNovice, ecologicalReviewIntermediate, ecologicalReviewAdvanced,
				communityReviewNovice, communityReviewIntermediate, communityReviewAdvanced);
	}

	private String appendDataQuality(String dataQuality, String note) {
		if (dataQuality == null || dataQuality.isBlank()) {
			return note;
		}
		if (dataQuality.contains(note)) {
			return dataQuality;
		}
		return dataQuality + " " + note;
	}

	private record ReferenceNormalization(String description, String shortDescription, String mediumTitle,
			String shortTitle, String summary, List<String> pros,
			List<String> cons, List<AiReview.AiAttribute> attributes, String dataQuality,
			String technicalOneline, String technicalShortReview, String ecologicalOneline, String communityOneline,
			List<String> pdfs, List<String> images, List<String> videos, List<String> socialLinks,
			String technicalReviewNovice, String technicalReviewIntermediate, String technicalReviewAdvanced,
			String ecologicalReviewNovice, String ecologicalReviewIntermediate, String ecologicalReviewAdvanced,
			String communityReviewNovice, String communityReviewIntermediate, String communityReviewAdvanced) {
	}

	private static class ReferenceNormalizer {
		private final int maxSourceNumber;
		private boolean removedReferences;

		private ReferenceNormalizer(int maxSourceNumber) {
			this.maxSourceNumber = maxSourceNumber;
		}

		private String normalize(String text) {
			if (text == null) {
				return null;
			}
			java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[(\\d+)]").matcher(text);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				int number = Integer.parseInt(matcher.group(1));
				if (number > maxSourceNumber || number <= 0) {
					removedReferences = true;
					matcher.appendReplacement(buffer, "");
				} else {
					matcher.appendReplacement(buffer, matcher.group(0));
				}
			}
			matcher.appendTail(buffer);
			return buffer.toString();
		}

		private boolean hasRemovedReferences() {
			return removedReferences;
		}
	}

	private boolean isExcludedDomain(String url) {
		if (url == null || properties.getExcludedDomains() == null) {
			return false;
		}
		// Basic check: if the URL contains any of the excluded domain strings.
		return properties.getExcludedDomains().stream().anyMatch(url::contains);
	}

}
