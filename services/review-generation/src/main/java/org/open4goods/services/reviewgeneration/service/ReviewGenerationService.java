package org.open4goods.services.reviewgeneration.service;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.open4goods.services.reviewgeneration.dto.AttributeExtractionResult;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationFailureDetails;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult;

import org.open4goods.model.ai.AiReview;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductFetchDiagnostics;
import org.open4goods.model.product.ProductReviewMetadata;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.review.ReviewGenerationStatus.Status;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.dto.BatchResultItem;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.exceptions.BatchJobFailedException;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

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
	private static final Pattern FIRST_NUMERIC_VALUE_PATTERN = Pattern.compile("[-+]?\\d+(?:[\\s.]\\d{3})*(?:[,.]\\d+)?|[-+]?\\d+(?:[,.]\\d+)?");
	private static final Pattern NUMBER_WITH_OPTIONAL_UNIT_PATTERN = Pattern.compile(
			"([-+]?\\d+(?:[\\s.]\\d{3})*(?:[,.]\\d+)?|[-+]?\\d+(?:[,.]\\d+)?)(?:\\s*([\\p{L}µ°%\"/]+))?");
	private static final Map<String, UnitDefinition> UNIT_DEFINITIONS = unitDefinitions();

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

    private final List<ReviewGenerationHook> hooks;

	private final ObjectMapper objectMapper;
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
            VerticalsConfigService verticalsConfigService, List<ReviewGenerationHook> hooks) {
		this.properties = properties;
		this.genAiService = genAiService;
		this.batchAiService = batchAiService;
		this.meterRegistry = meterRegistry;
		this.productRepository = productRepository;
        this.preprocessingService = preprocessingService;
        this.verticalsConfigService = verticalsConfigService;
        this.hooks = hooks;
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
		int maxCacheSize = properties.getUrlCacheMaxSize();
		this.urlCache = Collections.synchronizedMap(new LinkedHashMap<>(maxCacheSize, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
				return size() > maxCacheSize;
			}
		});
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
	 * (e.g. > 20 chars). Product attributes are managed by the dedicated extraction
	 * stage and are intentionally not part of the persisted review validity check.
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
		if (review.getShortDescription() == null || review.getShortDescription().isBlank()) {
			return false;
		}
		if (review.getTechnicalOneline() == null || review.getTechnicalOneline().isBlank()) {
			return false;
		}
		if (review.getEcologicalOneline() == null || review.getEcologicalOneline().isBlank()) {
			return false;
		}
		if (review.getCommunityOneline() == null || review.getCommunityOneline().isBlank()) {
			return false;
		}
		return true;
	}



    /**
     * Generates the prompt configuration for review generation without executing the AI call.
     * <p>
     * This method performs the necessary preprocessing to build the prompt variables (fetching data,
     * aggregating content) and then resolves the prompt configuration.
     * </p>
     *
     * @param product        the product.
     * @param verticalConfig the vertical configuration.
     * @return the resolved {@link PromptConfig}.
     * @throws ResourceNotFoundException if the prompt configuration or product data is missing.
     */
    public PromptConfig generateReviewDryRun(Product product, VerticalConfig verticalConfig) throws Exception {
        // Use a dummy status as we are not tracking this process
        ReviewGenerationStatus status = new ReviewGenerationStatus();
        status.setUpc(product.getId());
        status.setGtin(product.gtin());
        status.setStatus(ReviewGenerationStatus.Status.PREPROCESSING);
        status.setStartTime(Instant.now().toEpochMilli());

        String promptKey = properties.getPromptKey();
        PromptConfig promptConfig = genAiService.getPromptConfig(promptKey);
        if (promptConfig == null) {
            throw new ResourceNotFoundException("Prompt not found: " + promptKey);
        }

        Map<String, Object> promptVariables = preprocessingService.preparePromptVariables(product, verticalConfig,
                status);

        // Add mock tokens if missing (mimic real execution)
        if (!promptVariables.containsKey("SOURCE_TOKENS")) {
             promptVariables.put("SOURCE_TOKENS", new HashMap<>());
        }
         if (!promptVariables.containsKey("TOTAL_TOKENS")) {
             promptVariables.put("TOTAL_TOKENS", 0);
        }

        return genAiService.resolvePrompt(promptKey, promptVariables, AiReview.class);
    }

	/**
	 * Runs only the remote-source fetching stage and persists accepted markdown facts
	 * on the product.
	 *
	 * @param product        the product to enrich
	 * @param verticalConfig the product vertical configuration
	 * @param customHeaders  request headers forwarded to URL fetching
	 * @return a synchronous step result
	 * @throws Exception when source discovery or persistence fails
	 */
	public ReviewGenerationStepResult fetchReviewSources(Product product, VerticalConfig verticalConfig,
			Map<String, String> customHeaders) throws Exception {
		ReviewGenerationStatus status = new ReviewGenerationStatus();
		status.setUpc(product.getId());
		status.setGtin(product.gtin());
		status.setStatus(ReviewGenerationStatus.Status.SEARCHING);
		status.setStartTime(Instant.now().toEpochMilli());
		boolean hadEprelBeforeFetch = hasEprel(product);

		Map<String, Object> promptVariables;
		try {
			promptVariables = preprocessingService.preparePromptVariables(product, verticalConfig,
					status, customHeaders == null ? Map.of() : customHeaders);
		} catch (NotEnoughDataException e) {
			Map<String, String> enrichmentStatus = runSourceFetchedHooks(product, hadEprelBeforeFetch);
			e.setEnrichmentStatus(enrichmentStatus);
			persistFetchDiagnostics(product, e.getDetails(), enrichmentStatus);
			productRepository.forceIndex(product);
			throw e;
		}
		Map<String, String> enrichmentStatus = runSourceFetchedHooks(product, hadEprelBeforeFetch);
		persistFetchDiagnostics(product, promptVariables, enrichmentStatus);
		productRepository.forceIndex(product);
		return stepResult(product, verticalConfig, "fetch", true, "Remote sources fetched and persisted.",
				promptVariables, List.of(), null, enrichmentStatus);
	}

	/**
	 * Runs only the attribute extraction stage using persisted review facts and
	 * persists extracted attributes on the product.
	 *
	 * @param product        the product to enrich
	 * @param verticalConfig the product vertical configuration
	 * @return a synchronous step result
	 * @throws Exception when extraction fails
	 */
	public ReviewGenerationStepResult extractReviewAttributes(Product product, VerticalConfig verticalConfig)
			throws Exception {
		Map<String, Object> promptVariables = preprocessingService.buildPromptVariablesFromReviewFacts(product,
				verticalConfig, true);
		List<AiReview.AiAttribute> attributes = extractAndPersistReviewAttributes(product, verticalConfig,
				promptVariables);
		productRepository.forceIndex(product);
		return stepResult(product, verticalConfig, "attributes", true, "Attributes extracted and persisted.",
				promptVariables, attributes, null);
	}

	/**
	 * Runs only the text completion stage from persisted facts and persisted product
	 * attributes.
	 *
	 * @param product        the product to review
	 * @param verticalConfig the product vertical configuration
	 * @return a synchronous step result
	 * @throws Exception when text generation fails
	 */
	public ReviewGenerationStepResult generateReviewText(Product product, VerticalConfig verticalConfig)
			throws Exception {
		Map<String, Object> promptVariables = preprocessingService.buildPromptVariablesFromReviewFacts(product,
				verticalConfig, true);
		List<AiReview.AiAttribute> attributes = aiAttributesFromProduct(product, verticalConfig);
		if (attributes.isEmpty()) {
			throw new IllegalStateException("No persisted product attributes available for UPC " + product.getId()
					+ ". Run the attribute extraction stage first.");
		}
		promptVariables.put("EXTRACTED_ATTRIBUTES", objectMapper.writeValueAsString(attributes));
		PromptResponse<AiReview> reviewResponse = genAiService.objectPrompt(properties.getPromptKey(),
				promptVariables, AiReview.class);
		AiReview newReview = processAiReview(reviewResponse.getBody(), reviewResponse.getMetadata());
		if (newReview == null) {
			throw new IllegalStateException("Text completion returned no AI review for UPC " + product.getId());
		}
		// Attributes are owned by the dedicated extraction stage. Do not persist
		// model-emitted attributes inside the review payload.
		newReview.setAttributes(List.of());
		addResources(product, newReview);

		AiReviewHolder holder = new AiReviewHolder();
		holder.setCreatedMs(Instant.now().toEpochMilli());
		holder.setReview(newReview);
		holder.setEnoughData(true);
		@SuppressWarnings("unchecked")
		Map<String, Integer> sourceTokens = (Map<String, Integer>) promptVariables.getOrDefault("SOURCE_TOKENS",
				new HashMap<>());
		holder.setSources(sourceTokens);
		holder.setTotalTokens((Integer) promptVariables.getOrDefault("TOTAL_TOKENS", 0));
		applyReview(product, holder);
		runHooksPreservingAiSources(product, ReviewGenerationHook::onReviewGenerated);
		productRepository.forceIndex(product);
		return stepResult(product, verticalConfig, "text", true, "Review text generated and persisted.",
				promptVariables, attributes, newReview);
	}

	/**
	 * Runs the full synchronous workflow: fetch, attributes, then text.
	 *
	 * @param product        the product to review
	 * @param verticalConfig the product vertical configuration
	 * @param customHeaders  request headers forwarded to URL fetching
	 * @return the text-generation result
	 * @throws Exception when any stage fails
	 */
	public ReviewGenerationStepResult generateReviewWorkflow(Product product, VerticalConfig verticalConfig,
			Map<String, String> customHeaders) throws Exception {
		fetchReviewSources(product, verticalConfig, customHeaders);
		extractReviewAttributes(product, verticalConfig);
		return generateReviewText(product, verticalConfig);
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

		int waiting = executorService.getQueue().size();
		ReviewGenerationStatus queueStatus = processStatusMap.get(upc);
		queueStatus.setStatus(ReviewGenerationStatus.Status.QUEUED);
		queueStatus.addMessage("Queued for execution. Number waiting in queue: " + waiting);

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
				String promptKey = properties.getPromptKey();
				PromptConfig promptConfig = genAiService.getPromptConfig(promptKey);
				if (promptConfig == null) {
					throw new ResourceNotFoundException("Prompt not found: " + promptKey);
				}
				status.setStatus(ReviewGenerationStatus.Status.SEARCHING);
				Map<String, Object> promptVariables = preprocessingService.preparePromptVariables(product,
						verticalConfig, status, customHeaders);

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
				if (properties.isTwoPhaseGeneration()) {
					reviewResponse = executeExternalSourcesTwoPhase(promptKey, promptVariables, status, product,
							verticalConfig);
				} else {
					reviewResponse = genAiService.objectPrompt(promptKey, promptVariables, AiReview.class);
				}
				AiReview newReview = processAiReview(reviewResponse.getBody(), reviewResponse.getMetadata());

				// Attributes are owned by the dedicated extraction stage. Do not persist
				// model-emitted attributes inside the review payload.
				newReview.setAttributes(List.of());
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
				logger.warn("Not enough data for generating AI review for UPC {}: {}", upc, e.getMessage());
				holder.setEnoughData(false);
				holder.setFailureReason("INSUFFICIENT_SOURCES");
				status.setResult(holder);
				status.setStatus(ReviewGenerationStatus.Status.FAILED);
				status.setErrorMessage("Not enough data to generate an AI review for this product.");
				status.addEvent(ReviewGenerationStatus.ProgressEventType.ERROR, status.getErrorMessage(), null);
				meterRegistry.counter("review.generation.failed").increment();
				lastGenerationFailed = true;

			} catch (Throwable e) {
				logger.error("Asynchronous review generation failed for UPC {}: {}", upc, e.getMessage(), e);
				holder.setEnoughData(false);
				holder.setFailureReason("GENERATION_ERROR");
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


            if (status.getStatus() == ReviewGenerationStatus.Status.SUCCESS) {
				runHooksPreservingAiSources(product, ReviewGenerationHook::onReviewGenerated);
            }

			applyReview(product, holder);
			productRepository.forceIndex(product);
		});
		return upc;
	}

	/**
	 * Stores the generated review holder for the French locale and refreshes the indexed review
	 * metadata so it stays consistent with the reviews it summarises.
	 *
	 * <p>The indexed {@link ProductReviewMetadata} is a pure projection of {@link Product#getReviews()}.
	 * The aggregation pipeline rebuilds it on every aggregation pass, but a review trigger mutates the
	 * reviews outside that pipeline, so the projection is re-applied here before the product is
	 * persisted. The persistence layer intentionally no longer performs this enrichment.</p>
	 *
	 * @param product the product whose reviews are being updated
	 * @param holder  the generated review holder to store
	 */
	private void applyReview(Product product, AiReviewHolder holder) {
		product.getReviews().put("fr", holder);
		product.rebuildReviewMetadata();
	}

	// -------------------- Batch Review Generation Methods -------------------- //

    /**
     * Evicts completed (SUCCESS or FAILED) process status entries that are older than
     * {@code review.generation.status-map-ttl-hours}. Runs hourly.
     */
    @Scheduled(fixedDelayString = "PT1H")
    public void evictStaleProcessStatuses() {
        Instant cutoff = Instant.now().minus(properties.getStatusMapTtlHours(), ChronoUnit.HOURS);
        int before = processStatusMap.size();
        processStatusMap.entrySet().removeIf(entry -> {
            ReviewGenerationStatus s = entry.getValue();
            boolean terminal = s.getStatus() == Status.SUCCESS || s.getStatus() == Status.FAILED
                    || s.getStatus() == Status.ALREADY_PROCESSED;
            if (!terminal) {
                return false;
            }
            Long endTime = s.getEndTime();
            return endTime != null && Instant.ofEpochMilli(endTime).isBefore(cutoff);
        });
        int evicted = before - processStatusMap.size();
        if (evicted > 0) {
            logger.info("Evicted {} stale process status entries (TTL={}h). Map size now: {}.",
                    evicted, properties.getStatusMapTtlHours(), processStatusMap.size());
        }
    }

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
                productRepository.exportVerticalWithValidDateAndMissingReviewOrderByImpactScore(
                        verticalConfig.getId(),
                        "fr",
                        selectionLimit,
                        false,
                        sortOnImpactScore,
                        properties.getRegenerationDelayDays(),
                        properties.getRetryDelayDays())) {
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
		String promptKey = properties.getPromptKey();
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
				Map<String, Object> vars = preprocessingService.preparePromptVariables(product, verticalConfig, status);
				if (properties.isTwoPhaseGeneration()) {
					injectExtractedAttributes(product, verticalConfig, vars);
				}
				promptVariablesList.add(vars);
				productIds.add(String.valueOf(product.getId()));
				gtins.add(product.gtin());
			} catch (NotEnoughDataException e) {
				logger.error("Not enough data while preparing prompt for UPC {}: {}", product.getId(), e.getMessage());
				AiReviewHolder holder = new AiReviewHolder();
				holder.setCreatedMs(Instant.now().toEpochMilli());
				holder.setEnoughData(false);
				holder.setFailureReason("INSUFFICIENT_SOURCES");
				applyReview(product, holder);
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

	/**
	 * Executes the two-phase LLM flow for EXTERNAL_SOURCES mode:
	 * <ol>
	 *   <li>Phase 1 — attribute extraction via {@code attributeExtractionPromptKey}.</li>
	 *   <li>Phase 2 — full review text generation with the extracted attributes injected.</li>
	 * </ol>
	 *
	 * @param textPromptKey   the prompt key for phase 2 text generation
	 * @param promptVariables shared variable map (mutated to add EXTRACTED_ATTRIBUTES)
	 * @param status          the process status to update with progress messages
	 * @param upc             the product UPC for log correlation
	 * @return the phase-2 {@link PromptResponse}
	 */
	private PromptResponse<AiReview> executeExternalSourcesTwoPhase(String textPromptKey,
			Map<String, Object> promptVariables, ReviewGenerationStatus status, Product product,
			VerticalConfig verticalConfig) throws Exception {
		String attrPromptKey = properties.getAttributeExtractionPromptKey();
		long upc = product.getId();
		logger.info("Phase 1 (attribute extraction) starting for UPC {} with promptKey '{}'.", upc, attrPromptKey);
		status.addMessage("Phase 1: extracting attributes...");

		List<AiReview.AiAttribute> extractedAttributes = extractAndPersistReviewAttributes(product, verticalConfig,
				promptVariables);
		logger.info("Phase 1 complete for UPC {}: {} attributes extracted.", upc, extractedAttributes.size());

		promptVariables.put("EXTRACTED_ATTRIBUTES", objectMapper.writeValueAsString(
				aiAttributesFromProduct(product, verticalConfig)));

		logger.info("Phase 2 (text generation) starting for UPC {} with promptKey '{}'.", upc, textPromptKey);
		status.addMessage("Phase 2: generating review text...");
		return genAiService.objectPrompt(textPromptKey, promptVariables, AiReview.class);
	}

	/**
	 * Runs attribute extraction for a single product and injects the result as
	 * {@code EXTRACTED_ATTRIBUTES} into the given variable map.
	 * Used by the batch preprocessing loop when two-phase generation is enabled.
	 *
	 * @param vars the variable map to enrich (mutated in place)
	 * @param upc  the product UPC for log correlation
	 */
	private void injectExtractedAttributes(Product product, VerticalConfig verticalConfig, Map<String, Object> vars) {
		String attrPromptKey = properties.getAttributeExtractionPromptKey();
		long upc = product.getId();
		try {
			logger.info("Batch phase 1 (attribute extraction) for UPC {} with promptKey '{}'.", upc, attrPromptKey);
			List<AiReview.AiAttribute> attrs = extractAndPersistReviewAttributes(product, verticalConfig, vars);
			vars.put("EXTRACTED_ATTRIBUTES", objectMapper.writeValueAsString(
					aiAttributesFromProduct(product, verticalConfig)));
			productRepository.forceIndex(product);
			logger.info("Batch phase 1 complete for UPC {}: {} attributes extracted.", upc, attrs.size());
		} catch (Exception e) {
			logger.warn("Batch phase 1 failed for UPC {}; proceeding with empty EXTRACTED_ATTRIBUTES: {}", upc, e.getMessage());
			vars.put("EXTRACTED_ATTRIBUTES", "[]");
		}
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
					// Attributes are owned by the dedicated extraction stage: carry the validated
					// attributes for serialisation, never merge text-model attributes into the product.
					VerticalConfig verticalConfig = verticalsConfigService
							.getConfigByIdOrDefault(product.getVertical());
					newReview.setAttributes(aiAttributesFromProduct(product, verticalConfig));

					AiReviewHolder holder = new AiReviewHolder();
					holder.setCreatedMs(Instant.now().toEpochMilli());
					holder.setReview(newReview);
					holder.setEnoughData(true);
					applyReview(product, holder);
					runHooksPreservingAiSources(product, ReviewGenerationHook::onReviewGenerated);
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

	private List<AiReview.AiAttribute> extractAndPersistReviewAttributes(Product product, VerticalConfig verticalConfig,
			Map<String, Object> promptVariables) throws Exception {
		PromptResponse<AttributeExtractionResult> attrResponse = genAiService.objectPrompt(
				properties.getAttributeExtractionPromptKey(), promptVariables, AttributeExtractionResult.class);
		List<AiReview.AiAttribute> extractedAttributes = attrResponse.getBody() == null ? List.of()
				: attrResponse.getBody().attributes();
		List<String> acceptedUrls = acceptedUrls(promptVariables);
		List<AiReview.AiAttribute> validatedAttributes = validateExtractedAttributes(extractedAttributes,
				verticalConfig, acceptedUrls, promptVariables);
		persistValidatedAttributes(product, validatedAttributes, acceptedUrls);
		runHooksPreservingAiSources(product, (hook, p) -> hook.onAttributesExtracted(p));
		return validatedAttributes;
	}

	private List<AiReview.AiAttribute> validateExtractedAttributes(List<AiReview.AiAttribute> attributes,
			VerticalConfig verticalConfig, List<String> acceptedUrls, Map<String, Object> promptVariables) {
		if (attributes == null || attributes.isEmpty()) {
			return List.of();
		}
		Map<String, AttributeConfig> attributeConfigs = canonicalAttributeConfigs(verticalConfig);
		Set<String> canonicalKeys = attributeConfigs.keySet();
		Map<String, String> sources = promptSources(promptVariables);
		List<AiReview.AiAttribute> valid = new ArrayList<>();
		for (AiReview.AiAttribute attribute : attributes) {
			if (attribute == null || attribute.getName() == null || attribute.getName().isBlank()
					|| attribute.getValue() == null || attribute.getValue().isBlank()) {
				continue;
			}
			String key = attribute.getName().trim();
			if (!canonicalKeys.contains(key)) {
				logger.warn("Rejecting AI review attribute with unknown canonical key '{}'.", key);
				continue;
			}
			Integer number = attribute.getNumber();
			if (number == null || number < 1 || number > acceptedUrls.size()) {
				logger.warn("Rejecting AI review attribute '{}' with invalid source number '{}'.", key, number);
				continue;
			}
			AttributeConfig attributeConfig = attributeConfigs.get(key);
			String value = normalizeExtractedAttributeValue(key, attribute.getValue(), attributeConfig);
			if (value == null || value.isBlank()) {
				continue;
			}
			Integer verifiedSourceNumber = verifiedSourceNumber(key, value, number, acceptedUrls, sources,
					attributeConfig);
			if (verifiedSourceNumber == null) {
				logger.warn("Rejecting AI review attribute '{}'='{}' because no accepted source contains supporting evidence.",
						key, value);
				continue;
			}
			valid.add(new AiReview.AiAttribute(key, value, verifiedSourceNumber));
		}
		return valid;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> promptSources(Map<String, Object> promptVariables) {
		if (promptVariables == null) {
			return Map.of();
		}
		Object sources = promptVariables.get("sources");
		if (sources instanceof Map<?, ?> sourceMap) {
			Map<String, String> ret = new LinkedHashMap<>();
			sourceMap.forEach((url, markdown) -> {
				if (url != null && markdown != null) {
					ret.put(url.toString(), markdown.toString());
				}
			});
			return ret;
		}
		return (Map<String, String>) promptVariables.getOrDefault("SOURCES", Map.of());
	}

	private String normalizeExtractedAttributeValue(String key, String value, AttributeConfig attributeConfig) {
		String trimmed = value == null ? null : value.trim();
		if (trimmed == null || trimmed.isBlank()) {
			return null;
		}
		if (AttributeType.BOOLEAN.equals(attributeConfig == null ? null : attributeConfig.getFilteringType())) {
			String v = trimmed.toLowerCase();
			if (v.equals("true") || v.equals("yes") || v.equals("oui") || v.equals("1") || v.equals("vrai")) return "TRUE";
			if (v.equals("false") || v.equals("no") || v.equals("non") || v.equals("0") || v.equals("faux")) return "FALSE";
			logger.warn("Rejecting AI review boolean attribute '{}'='{}' — not a recognized boolean synonym.", key, value);
			return null;
		}
		if (!isNumericAttribute(attributeConfig)) {
			return trimmed;
		}
		String numericValue = firstNumericToken(trimmed);
		if (numericValue == null) {
			logger.warn("Rejecting AI review numeric attribute '{}'='{}' because it does not contain a numeric value.",
					key, value);
			return null;
		}
		// Reject non-positive numerics: a spec value of 0 (or negative) is virtually always a
		// failed parse or hallucination (e.g. FREQUENCY_RATE=0 for a 60 Hz TV), never a real
		// product characteristic for the numeric attributes we track.
		try {
			double parsed = Double.parseDouble(numericValue.replace(',', '.'));
			if (parsed <= 0d) {
				logger.warn("Rejecting AI review numeric attribute '{}'='{}' because its value is not strictly positive.",
						key, value);
				return null;
			}
		} catch (NumberFormatException e) {
			logger.warn("Rejecting AI review numeric attribute '{}'='{}' because it is not a parsable number.", key, value);
			return null;
		}
		return numericValue;
	}

	private Integer verifiedSourceNumber(String key, String value, Integer proposedNumber, List<String> acceptedUrls,
			Map<String, String> sources, AttributeConfig attributeConfig) {
		if (sources == null || sources.isEmpty() || isTriviallyVerifiable(value)) {
			return proposedNumber;
		}
		String proposedUrl = acceptedUrls.get(proposedNumber - 1);
		if (sourceContainsAttributeEvidence(sources.get(proposedUrl), key, value, attributeConfig)) {
			return proposedNumber;
		}
		for (int i = 0; i < acceptedUrls.size(); i++) {
			String url = acceptedUrls.get(i);
			if (sourceContainsAttributeEvidence(sources.get(url), key, value, attributeConfig)) {
				logger.info("Reassigned AI review attribute '{}' source from {} to {} after source-content verification.",
						key, proposedNumber, i + 1);
				return i + 1;
			}
		}
		return null;
	}

	private boolean isTriviallyVerifiable(String value) {
		if (value == null) {
			return true;
		}
		String normalized = normalizeEvidence(value);
		return normalized.length() <= 1 || "true".equals(normalized) || "false".equals(normalized);
	}

	private boolean sourceContainsAttributeEvidence(String markdown, String key, String value,
			AttributeConfig attributeConfig) {
		if (markdown == null || markdown.isBlank() || value == null || value.isBlank()) {
			return false;
		}
		String normalizedMarkdown = normalizeEvidence(markdown);
		String normalizedValue = normalizeEvidence(value);
		if (normalizedValue.isBlank()) {
			return true;
		}
		if (normalizedMarkdown.contains(normalizedValue)) {
			return true;
		}
		String compactValue = normalizedValue.replace(" ", "");
		if (compactValue.length() >= 3 && normalizedMarkdown.replace(" ", "").contains(compactValue)) {
			return true;
		}
		return isNumericAttribute(attributeConfig) && sourceContainsNumericEvidence(markdown, value, attributeConfig);
	}

	private String normalizeEvidence(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replace('\u00a0', ' ')
				.replaceAll("[^\\p{Alnum}]+", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private boolean sourceContainsNumericEvidence(String markdown, String value, AttributeConfig attributeConfig) {
		String expectedToken = firstNumericToken(value);
		if (expectedToken == null) {
			return false;
		}
		Double expected = parseDouble(expectedToken);
		if (expected == null) {
			return false;
		}
		String targetUnit = targetUnit(attributeConfig);
		ConvertedNumber expectedConverted = convert(expected, targetUnit, dimension(attributeConfig));
		Matcher matcher = NUMBER_WITH_OPTIONAL_UNIT_PATTERN.matcher(markdown.replace('\u00a0', ' '));
		while (matcher.find()) {
			String sourceNumberToken = firstNumericToken(matcher.group(1));
			Double sourceValue = parseDouble(sourceNumberToken);
			if (sourceValue == null) {
				continue;
			}
			if (sameNumericValue(sourceValue, expected)) {
				return true;
			}
			String sourceUnit = matcher.group(2);
			if (sourceUnit == null || sourceUnit.isBlank() || expectedConverted == null) {
				continue;
			}
			ConvertedNumber sourceConverted = convert(sourceValue, sourceUnit, dimension(attributeConfig));
			if (sourceConverted != null && expectedConverted.dimension().equals(sourceConverted.dimension())
					&& sameNumericValue(expectedConverted.baseValue(), sourceConverted.baseValue())) {
				return true;
			}
		}
		return false;
	}

	private String firstNumericToken(String value) {
		if (value == null) {
			return null;
		}
		Matcher matcher = FIRST_NUMERIC_VALUE_PATTERN.matcher(value.replace('\u00a0', ' '));
		if (!matcher.find()) {
			return null;
		}
		String token = matcher.group();
		String normalized = token.replace(" ", "");
		if (normalized.indexOf(',') >= 0 && normalized.indexOf('.') >= 0) {
			normalized = normalized.replace(".", "").replace(',', '.');
		} else {
			normalized = normalized.replace(',', '.');
		}
		try {
			return new BigDecimal(normalized).stripTrailingZeros().toPlainString();
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Double parseDouble(String value) {
		try {
			return value == null ? null : Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private boolean sameNumericValue(double left, double right) {
		double tolerance = Math.max(0.000001d, Math.abs(right) * 0.000001d);
		return Math.abs(left - right) <= tolerance;
	}

	private boolean isNumericAttribute(AttributeConfig attributeConfig) {
		return attributeConfig != null && AttributeType.NUMERIC.equals(attributeConfig.getFilteringType());
	}

	private String targetUnit(AttributeConfig attributeConfig) {
		if (attributeConfig == null) {
			return null;
		}
		if (attributeConfig.getParser() != null && attributeConfig.getParser().getDefaultUnitHint() != null
				&& !attributeConfig.getParser().getDefaultUnitHint().isBlank()) {
			return attributeConfig.getParser().getDefaultUnitHint();
		}
		String suffix = firstLocalisedValue(attributeConfig.getSuffix());
		if (suffix != null && !suffix.isBlank()) {
			return suffix;
		}
		return firstLocalisedValue(attributeConfig.getUnit());
	}

	private String firstLocalisedValue(Map<String, String> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		String preferred = values.get("default");
		if (preferred == null) {
			preferred = values.get("fr");
		}
		if (preferred != null) {
			return preferred;
		}
		return values.values().stream().filter(Objects::nonNull).findFirst().orElse(null);
	}

	private String dimension(AttributeConfig attributeConfig) {
		if (attributeConfig != null && attributeConfig.getParser() != null
				&& attributeConfig.getParser().getDimension() != null
				&& !attributeConfig.getParser().getDimension().isBlank()) {
			return attributeConfig.getParser().getDimension();
		}
		String targetUnit = targetUnit(attributeConfig);
		UnitDefinition definition = UNIT_DEFINITIONS.get(normalizeUnit(targetUnit));
		return definition == null ? null : definition.dimension();
	}

	private ConvertedNumber convert(double value, String unit, String expectedDimension) {
		UnitDefinition definition = UNIT_DEFINITIONS.get(normalizeUnit(unit));
		if (definition == null) {
			return null;
		}
		if (expectedDimension != null && !expectedDimension.isBlank()
				&& !expectedDimension.equalsIgnoreCase(definition.dimension())) {
			return null;
		}
		return new ConvertedNumber(definition.dimension(), value * definition.toBaseFactor());
	}

	private String normalizeUnit(String unit) {
		if (unit == null) {
			return "";
		}
		return unit.trim()
				.replace('\u00a0', ' ')
				.replace("²", "2")
				.replace("³", "3")
				.toLowerCase(Locale.ROOT);
	}

	private static Map<String, UnitDefinition> unitDefinitions() {
		Map<String, UnitDefinition> definitions = new HashMap<>();
		addUnit(definitions, "LENGTH", 0.001d, "mm", "millimetre", "millimetres", "millimeter", "millimeters");
		addUnit(definitions, "LENGTH", 0.01d, "cm", "centimetre", "centimetres", "centimeter", "centimeters");
		addUnit(definitions, "LENGTH", 1d, "m", "metre", "metres", "meter", "meters");
		addUnit(definitions, "LENGTH", 0.0254d, "\"", "in", "inch", "inches", "pouce", "pouces");
		addUnit(definitions, "MASS", 0.001d, "g", "gramme", "grammes", "gram", "grams");
		addUnit(definitions, "MASS", 1d, "kg", "kilogramme", "kilogrammes", "kilogram", "kilograms");
		addUnit(definitions, "MASS", 0.45359237d, "lb", "lbs", "pound", "pounds");
		addUnit(definitions, "POWER", 1d, "w", "watt", "watts");
		addUnit(definitions, "POWER", 1000d, "kw", "kilowatt", "kilowatts");
		addUnit(definitions, "ENERGY", 0.001d, "wh");
		addUnit(definitions, "ENERGY", 1d, "kwh");
		addUnit(definitions, "VOLUME", 0.001d, "ml", "millilitre", "millilitres", "milliliter", "milliliters");
		addUnit(definitions, "VOLUME", 0.01d, "cl", "centilitre", "centilitres", "centiliter", "centiliters");
		addUnit(definitions, "VOLUME", 1d, "l", "litre", "litres", "liter", "liters");
		addUnit(definitions, "SOUND_LEVEL", 1d, "db", "db(a)", "decibel", "decibels");
		addUnit(definitions, "FREQUENCY", 1d, "hz", "hertz");
		addUnit(definitions, "FREQUENCY", 1_000d, "khz", "kilohertz");
		addUnit(definitions, "FREQUENCY", 1_000_000d, "mhz", "megahertz");
		addUnit(definitions, "FREQUENCY", 1_000_000_000d, "ghz", "gigahertz");
		addUnit(definitions, "TEMPERATURE", 1d, "°c", "celsius", "degre c", "degree c");
		addUnit(definitions, "VOLTAGE", 1d, "v", "volt", "volts");
		addUnit(definitions, "VOLTAGE", 1_000d, "kv", "kilovolt");
		addUnit(definitions, "VOLTAGE", 0.001d, "mv", "millivolt");
		return Map.copyOf(definitions);
	}

	private static void addUnit(Map<String, UnitDefinition> definitions, String dimension, double toBaseFactor,
			String... symbols) {
		for (String symbol : symbols) {
			definitions.put(symbol, new UnitDefinition(dimension, toBaseFactor));
		}
	}

	private record UnitDefinition(String dimension, double toBaseFactor) {
	}

	private record ConvertedNumber(String dimension, double baseValue) {
	}

	private Map<String, AttributeConfig> canonicalAttributeConfigs(VerticalConfig verticalConfig) {
		if (verticalConfig == null || verticalConfig.getAttributesConfig() == null
				|| verticalConfig.getAttributesConfig().getConfigs() == null) {
			return Map.of();
		}
		return verticalConfig.getAttributesConfig().getConfigs().stream()
				.filter(Objects::nonNull)
				.filter(config -> config.getKey() != null && !config.getKey().isBlank())
				.collect(java.util.stream.Collectors.toMap(AttributeConfig::getKey, config -> config,
						(left, right) -> left, LinkedHashMap::new));
	}

	private Set<String> canonicalAttributeKeys(VerticalConfig verticalConfig) {
		return canonicalAttributeConfigs(verticalConfig).keySet();
	}

	private void persistValidatedAttributes(Product product, List<AiReview.AiAttribute> attributes,
			List<String> acceptedUrls) {
		removePreviousAiReviewSources(product);
		if (attributes == null || attributes.isEmpty()) {
			return;
		}
		for (AiReview.AiAttribute attribute : attributes) {
			String datasource = aiReviewDatasourceName(attribute.getNumber(), acceptedUrls);
			ProductAttribute productAttribute = product.getAttributes().getAll().get(attribute.getName());
			if (productAttribute == null) {
				productAttribute = new ProductAttribute();
				productAttribute.setName(attribute.getName());
			}
			productAttribute.addSourceAttribute(new SourcedAttribute(
					new Attribute(attribute.getName(), attribute.getValue(), "fr"), datasource));
			product.getAttributes().getAll().put(productAttribute.getName(), productAttribute);
		}
	}

	private void removePreviousAiReviewSources(Product product) {
		if (product == null || product.getAttributes() == null || product.getAttributes().getAll() == null) {
			return;
		}
		product.getAttributes().getAll().entrySet().removeIf(entry -> {
			ProductAttribute attribute = entry.getValue();
			if (attribute == null || attribute.getSource() == null) {
				return false;
			}
			attribute.getSource().removeIf(source -> isReviewAiSource(source == null ? null : source.getDataSourcename()));
			attribute.setValue(attribute.bestValue());
			return attribute.getSource().isEmpty();
		});
	}

	private Map<String, List<SourcedAttribute>> snapshotAiReviewSources(Product product) {
		if (product == null || product.getAttributes() == null || product.getAttributes().getAll() == null) {
			return Map.of();
		}
		Map<String, List<SourcedAttribute>> snapshot = new LinkedHashMap<>();
		product.getAttributes().getAll().forEach((key, attribute) -> {
			if (attribute == null || attribute.getSource() == null) {
				return;
			}
			List<SourcedAttribute> aiSources = attribute.getSource().stream()
					.filter(source -> isReviewAiSource(source == null ? null : source.getDataSourcename()))
					.map(this::copySourcedAttribute)
					.toList();
			if (!aiSources.isEmpty()) {
				snapshot.put(key, aiSources);
			}
		});
		return snapshot;
	}

	private void restoreMissingAiReviewSources(Product product, Map<String, List<SourcedAttribute>> snapshot) {
		if (product == null || product.getAttributes() == null || snapshot == null || snapshot.isEmpty()) {
			return;
		}
		snapshot.forEach((key, sources) -> {
			if (key == null || key.isBlank() || sources == null || sources.isEmpty()) {
				return;
			}
			ProductAttribute attribute = product.getAttributes().getAll().get(key);
			if (attribute == null) {
				attribute = new ProductAttribute();
				attribute.setName(key);
			}
			for (SourcedAttribute source : sources) {
				if (source == null || source.getDataSourcename() == null) {
					continue;
				}
				boolean alreadyPresent = attribute.getSource() != null && attribute.getSource().stream()
						.anyMatch(existing -> source.getDataSourcename().equals(existing.getDataSourcename()));
				if (!alreadyPresent) {
					attribute.addSourceAttribute(copySourcedAttribute(source));
				}
			}
			product.getAttributes().getAll().put(attribute.getName(), attribute);
		});
	}

	private SourcedAttribute copySourcedAttribute(SourcedAttribute source) {
		SourcedAttribute copy = new SourcedAttribute();
		copy.setDataSourcename(source.getDataSourcename());
		copy.setName(source.getName());
		copy.setValue(source.getValue());
		copy.setCleanedValue(source.getCleanedValue());
		copy.setIcecatTaxonomyId(source.getIcecatTaxonomyId());
		return copy;
	}

	private boolean isReviewAiSource(String datasource) {
		if (datasource == null || datasource.isBlank()) {
			return false;
		}
		String normalized = datasource.trim();
		String upper = normalized.toUpperCase(java.util.Locale.ROOT);
		String lower = normalized.toLowerCase(java.util.Locale.ROOT);
		return upper.startsWith("AI_REVIEW:") || upper.equals("AI") || upper.equals("OPEN_AI")
				|| upper.equals("GEMINI") || lower.startsWith("gpt-") || lower.startsWith("gemini-");
	}

	private String aiReviewDatasourceName(Integer number, List<String> acceptedUrls) {
		String url = number == null || number < 1 || number > acceptedUrls.size() ? "" : acceptedUrls.get(number - 1);
		String host = hostOf(url);
		return "AI_REVIEW:s" + String.format("%02d", number) + ":" + host;
	}

	private String hostOf(String url) {
		try {
			return URI.create(url).toURL().getHost();
		} catch (Exception e) {
			return "unknown";
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> acceptedUrls(Map<String, Object> promptVariables) {
		if (promptVariables == null) {
			return List.of();
		}
		Object urls = promptVariables.get("ACCEPTED_URLS");
		if (urls instanceof List<?> list) {
			return list.stream()
					.filter(Objects::nonNull)
					.map(Object::toString)
					.toList();
		}
		Map<String, Integer> sourceTokens = (Map<String, Integer>) promptVariables.getOrDefault("SOURCE_TOKENS", Map.of());
		return new ArrayList<>(sourceTokens.keySet());
	}

	/**
	 * Snapshots AI_REVIEW-sourced attributes, runs hook actions (errors logged, never thrown),
	 * then restores any sources that hooks may have dropped.
	 */
	private void runHooksPreservingAiSources(Product product, BiConsumer<ReviewGenerationHook, Product> hookAction) {
		Map<String, List<SourcedAttribute>> snapshot = snapshotAiReviewSources(product);
		try {
			hooks.forEach(hook -> hookAction.accept(hook, product));
		} catch (Exception e) {
			logger.error("Error executing hooks for UPC {}: {}", product.getId(), e.getMessage(), e);
		}
		restoreMissingAiReviewSources(product, snapshot);
	}

	private List<AiReview.AiAttribute> aiAttributesFromProduct(Product product, VerticalConfig verticalConfig) {
		if (product.getAttributes() == null) {
			return List.of();
		}
		Set<String> canonicalKeys = canonicalAttributeKeys(verticalConfig);
		Map<String, AiReview.AiAttribute> resolvedAttributes = new LinkedHashMap<>();
		if (product.getAttributes().getIndexed() != null) {
			product.getAttributes().getIndexed().values().stream()
					.filter(attribute -> canonicalKeys.contains(attribute.getName()))
					.filter(attribute -> attribute.getName() != null && !attribute.getName().isBlank())
					.filter(attribute -> attribute.getValue() != null && !attribute.getValue().isBlank())
					.forEach(attribute -> resolvedAttributes.put(attribute.getName(),
							new AiReview.AiAttribute(attribute.getName(), attribute.getValue(),
									sourceNumberFromIndexed(attribute))));
		}
		if (product.getAttributes().getAll() == null || product.getAttributes().getAll().isEmpty()) {
			return new ArrayList<>(resolvedAttributes.values());
		}
		product.getAttributes().getAll().values().stream()
				.filter(attribute -> canonicalKeys.contains(attribute.getName()))
				.filter(attribute -> attribute.getName() != null && !attribute.getName().isBlank())
				.filter(attribute -> attribute.getValue() != null && !attribute.getValue().isBlank())
				.forEach(attribute -> resolvedAttributes.putIfAbsent(attribute.getName(),
						new AiReview.AiAttribute(attribute.getName(), attribute.getValue(),
								sourceNumberFromProductAttribute(attribute))));
		return new ArrayList<>(resolvedAttributes.values());
	}

	private Integer sourceNumberFromIndexed(IndexedAttribute attribute) {
		return sourceNumberFromSources(attribute == null ? Collections.emptySet() : attribute.getSource());
	}

	private Integer sourceNumberFromProductAttribute(ProductAttribute attribute) {
		return sourceNumberFromSources(attribute == null ? Collections.emptySet() : attribute.getSource());
	}

	private Integer sourceNumberFromSources(Set<SourcedAttribute> sources) {
		if (sources == null || sources.isEmpty()) {
			return 1;
		}
		return sources.stream()
				.map(source -> parseAiReviewSourceNumber(source.getDataSourcename()))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(1);
	}

	private Integer parseAiReviewSourceNumber(String datasource) {
		if (datasource == null || !datasource.startsWith("AI_REVIEW:s") || datasource.length() < 12) {
			return null;
		}
		try {
			int end = datasource.indexOf(':', "AI_REVIEW:s".length());
			String value = end < 0 ? datasource.substring("AI_REVIEW:s".length())
					: datasource.substring("AI_REVIEW:s".length(), end);
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private ReviewGenerationStepResult stepResult(Product product, VerticalConfig verticalConfig, String step,
			boolean success, String message, Map<String, Object> promptVariables,
			List<AiReview.AiAttribute> attributes, AiReview review) {
		return stepResult(product, verticalConfig, step, success, message, promptVariables, attributes, review, Map.of());
	}

	@SuppressWarnings("unchecked")
	private ReviewGenerationStepResult stepResult(Product product, VerticalConfig verticalConfig, String step,
			boolean success, String message, Map<String, Object> promptVariables,
			List<AiReview.AiAttribute> attributes, AiReview review, Map<String, String> enrichmentStatus) {
		Map<String, Integer> sourceTokens = promptVariables == null ? Map.of()
				: (Map<String, Integer>) promptVariables.getOrDefault("SOURCE_TOKENS", Map.of());
		int totalTokens = promptVariables == null ? 0
				: (Integer) promptVariables.getOrDefault("TOTAL_TOKENS", 0);
		List<String> searchedQueries = promptVariables == null ? List.of()
				: (List<String>) promptVariables.getOrDefault("SEARCHED_QUERIES", List.of());
		List<String> acceptedUrls = promptVariables == null ? new ArrayList<>(sourceTokens.keySet())
				: (List<String>) promptVariables.getOrDefault("ACCEPTED_URLS", new ArrayList<>(sourceTokens.keySet()));
		Map<String, String> rejectedUrls = promptVariables == null ? Map.of()
				: (Map<String, String>) promptVariables.getOrDefault("REJECTED_URLS", Map.of());
		String resultQuality = promptVariables == null ? "UNKNOWN"
				: (String) promptVariables.getOrDefault("RESULT_QUALITY", "UNKNOWN");
		return new ReviewGenerationStepResult(product.getId(), product.gtin(),
				verticalConfig == null ? product.getVertical() : verticalConfig.getId(), step, success, message,
				sourceTokens.size(), totalTokens, resultQuality, attributes, review, null, searchedQueries, acceptedUrls,
				rejectedUrls, enrichmentStatus);
	}

	private boolean hasEprel(Product product) {
		return product != null && product.getExternalIds() != null && product.getExternalIds().getEprel() != null;
	}

	private Map<String, String> runSourceFetchedHooks(Product product, boolean hadEprelBeforeFetch) {
		Map<String, String> enrichmentStatus = new LinkedHashMap<>();
		enrichmentStatus.put("eprel.beforeFetch", Boolean.toString(hadEprelBeforeFetch));
		try {
			hooks.forEach(hook -> hook.onSourcesFetched(product, hadEprelBeforeFetch));
			boolean hasEprelAfterFetchHooks = hasEprel(product);
			enrichmentStatus.put("eprel.afterFetchHooks", Boolean.toString(hasEprelAfterFetchHooks));
			enrichmentStatus.put("eprel.status", hadEprelBeforeFetch ? "already_present"
					: hasEprelAfterFetchHooks ? "completed" : "not_completed");
			if (!hadEprelBeforeFetch && !hasEprelAfterFetchHooks) {
				logger.warn("EPREL data still missing for UPC {} after onSourcesFetched hooks. "
						+ "This product may lack energy-label attributes.", product.getId());
			}
		} catch (Exception e) {
			logger.error("Error executing onSourcesFetched hooks for UPC {}: {}", product.getId(), e.getMessage(), e);
			enrichmentStatus.put("eprel.afterFetchHooks", Boolean.toString(hasEprel(product)));
			enrichmentStatus.put("eprel.status", "hook_error");
			enrichmentStatus.put("eprel.error", e.getMessage());
		}
		return enrichmentStatus;
	}

	@SuppressWarnings("unchecked")
	private void persistFetchDiagnostics(Product product, Map<String, Object> promptVariables,
			Map<String, String> enrichmentStatus) {
		if (product == null || promptVariables == null) {
			return;
		}
		Map<String, Integer> sourceTokens = (Map<String, Integer>) promptVariables.getOrDefault("SOURCE_TOKENS", Map.of());
		int totalTokens = (Integer) promptVariables.getOrDefault("TOTAL_TOKENS", 0);
		List<String> searchedQueries = (List<String>) promptVariables.getOrDefault("SEARCHED_QUERIES", List.of());
		List<String> acceptedUrls = (List<String>) promptVariables.getOrDefault("ACCEPTED_URLS",
				new ArrayList<>(sourceTokens.keySet()));
		Map<String, String> sourceClasses = (Map<String, String>) promptVariables.getOrDefault("SOURCE_CLASSES", Map.of());
		Map<String, String> rejectedUrls = (Map<String, String>) promptVariables.getOrDefault("REJECTED_URLS", Map.of());
		String resultQuality = (String) promptVariables.getOrDefault("RESULT_QUALITY", "UNKNOWN");
		persistFetchDiagnostics(product, sourceTokens.size(), totalTokens, searchedQueries, acceptedUrls, sourceClasses,
				rejectedUrls, enrichmentStatus, resultQuality);
	}

	private void persistFetchDiagnostics(Product product, ReviewGenerationFailureDetails details,
			Map<String, String> enrichmentStatus) {
		if (product == null || details == null) {
			return;
		}
		persistFetchDiagnostics(product, details.sourceCount(), details.totalTokens(), details.searchedQueries(),
				details.acceptedUrls(), details.sourceClasses(), details.rejectedUrls(), enrichmentStatus, "FAILED");
	}

	private void persistFetchDiagnostics(Product product, int sourceCount, int totalTokens, List<String> searchedQueries,
			List<String> acceptedUrls, Map<String, String> sourceClasses, Map<String, String> rejectedUrls,
			Map<String, String> enrichmentStatus, String resultQuality) {
		ProductFetchDiagnostics diagnostics = new ProductFetchDiagnostics();
		diagnostics.setFetchedAt(Instant.now().toEpochMilli());
		diagnostics.setSourceCount(sourceCount);
		diagnostics.setTotalTokens(totalTokens);
		diagnostics.setResultQuality(resultQuality);
		diagnostics.setSearchedQueries(searchedQueries);
		diagnostics.setAcceptedUrls(acceptedUrls);
		diagnostics.setSourceClasses(sourceClasses);
		diagnostics.setRejectedUrls(rejectedUrls);
		diagnostics.setEnrichmentStatus(enrichmentStatus);
		product.setReviewFetchDiagnostics(diagnostics);
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
		// 1. Resolve sources from metadata (Grounding)
		List<AiReview.AiSource> metadataSources = resolveSourcesFromMetadata(metadata);
		
		// 2. Get AI-generated sources and filter out invalid/empty ones.
		List<AiReview.AiSource> aiSources = review.getSources() != null 
				? review.getSources() 
				: List.of();

		// 3. Merge: Grounding Sources (Priority) + AI Sources (Fallback/Supplementary)
		// We use a Map to deduct duplicates by URL.
		Map<String, AiReview.AiSource> mergedMap = new LinkedHashMap<>();
		int index = 1;

		// Add Metadata Sources first
		for (AiReview.AiSource src : metadataSources) {
			if (src.getUrl() != null && !mergedMap.containsKey(src.getUrl())) {
				// Re-index
				mergedMap.put(src.getUrl(), new AiReview.AiSource(index++, src.getName(), src.getDescription(), src.getUrl()));
			}
		}

		// Add AI Sources if not present
		for (AiReview.AiSource src : aiSources) {
			if (src.getUrl() != null && !mergedMap.containsKey(src.getUrl())) {
				mergedMap.put(src.getUrl(), new AiReview.AiSource(index++, src.getName(), src.getDescription(), src.getUrl()));
			}
		}

		List<AiReview.AiSource> finalSources = new ArrayList<>(mergedMap.values());
		
		// If no sources found at all, return original review (or maybe empty list)
		if (finalSources.isEmpty()) {
			return review;
		}

		// 4. Normalize references in the text to match the new indices
		// Note: This relies on the assumption that if text used [1], it meant the first source we now have.
		// If we prepended grounding sources, [1] now points to grounding source #1.
		ReferenceNormalization normalization = normalizeReferences(review, finalSources, finalSources.size());

		return copyWithNewData(review, normalization, finalSources);
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
				normalization.baseLine(),
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
				normalization.obsolescenceWarning(),
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
				normalizeText(review.getBaseLine()),
				normalizeText(review.getObsolescenceWarning()),
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
			// NOTE: do NOT drop sources on a live HEAD reachability check here. In EXTERNAL_SOURCES mode
			// these URLs are our own fetched sources (already retrieved successfully at the fetch stage),
			// and the model cites them by number. Many sites block bot HEAD requests, so re-validating
			// produced false negatives that emptied the sources list and left dangling [n] citations.

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
				review.getBaseLine(),
				review.getObsolescenceWarning(),
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
	/**
	 * Bounded LRU cache for resolved URLs. Access-order LinkedHashMap keeps the most-recently-used
	 * entries and evicts the oldest once {@code urlCacheMaxSize} is reached.
	 */
	private final Map<String, String> urlCache;

	/**
	 * Resolves a URL by following redirects (up to a limit).
	 * Caches results to improve performance.
	 *
	 * @param urlString the URL to resolve
	 * @return the final URL after following redirects, or the original if no redirect or error
	 */
	private String resolveUrl(String urlString) {
		if (urlString == null || urlString.isBlank()) {
			return urlString;
		}

		// Check cache first
		if (urlCache.containsKey(urlString)) {
			return urlCache.get(urlString);
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

			// Cache the result (even if it's the same as original)
			urlCache.put(urlString, currentUrl);
			return currentUrl;

		} catch (Exception e) {
			logger.warn("Failed to resolve URL {}: {}", urlString, e.getMessage());
			// Cache the failure (as original URL) to avoid repeated failures?
			// Maybe short term, but for now safe to just return original.
			return urlString; 
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
				replaceReferences(review.getBaseLine()),
				replaceReferences(review.getObsolescenceWarning()),
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

	private AiReview applyCitationsAndNormalize(AiReview review, Map<String, Object> metadata) {
		return processAiReview(review, metadata);
	}

	@SuppressWarnings("unchecked")
	private List<AiReview.AiSource> resolveSourcesFromMetadata(Map<String, Object> metadata) {
		List<AiReview.AiSource> sources = new ArrayList<>();
		if (metadata == null || metadata.isEmpty()) {
			return sources;
		}
		
		// 1. Try "groundingMetadata" (Gemini Native)
		// Structure: { "groundingChunks": [ { "web": { "uri": "...", "title": "..." } } ] }
		if (metadata.containsKey("groundingMetadata")) {
			Object groundingMetaObj = metadata.get("groundingMetadata");
			if (groundingMetaObj instanceof Map) {
				Map<String, Object> groundingMeta = (Map<String, Object>) groundingMetaObj;
				if (groundingMeta.get("groundingChunks") instanceof List<?> chunks) {
					int index = 1;
					for (Object chunkObj : chunks) {
						if (chunkObj instanceof Map) {
							Map<String, Object> chunk = (Map<String, Object>) chunkObj;
							if (chunk.get("web") instanceof Map) {
								Map<String, Object> web = (Map<String, Object>) chunk.get("web");
								String uri = (String) web.get("uri");
								String title = (String) web.get("title");
								if (uri != null && !uri.isBlank()) {
									// Title fallback
									if (title == null) title = ""; // or uri
									// For description, Gemini might not provide snippet in this structure easily.
									sources.add(new AiReview.AiSource(index++, title, "", uri));
								}
							}
						}
					}
				}
			}
		}

		// If we found grounding sources, return them.
		if (!sources.isEmpty()) {
			return sources;
		}

		// 2. Fallback: "citations" (Legacy / Generic Spring AI)
		Object citations = metadata.get(CITATIONS_METADATA_KEY);
		if (citations instanceof List<?> citationList) {
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
		String baseLine = normalizer.normalize(review.getBaseLine());
		String obsolescenceWarning = normalizer.normalize(review.getObsolescenceWarning());
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
				baseLine, obsolescenceWarning,
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
			String shortTitle, String baseLine, String obsolescenceWarning, String summary, List<String> pros,
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
