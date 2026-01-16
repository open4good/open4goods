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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.open4goods.model.ai.AiReview;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.review.ReviewGenerationStatus.Status;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.dto.openai.BatchOutput;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.prompt.service.provider.ProviderEvent;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
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
	 */
	public ReviewGenerationService(ReviewGenerationConfig properties, GoogleSearchService googleSearchService,
			UrlFetchingService urlFetchingService, PromptService genAiService, BatchPromptService batchAiService,
			MeterRegistry meterRegistry, ProductRepository productRepository, ReviewGenerationPreprocessingService preprocessingService) {
		this.properties = properties;
		this.genAiService = genAiService;
		this.batchAiService = batchAiService;
		this.meterRegistry = meterRegistry;
		this.productRepository = productRepository;
		this.preprocessingService = preprocessingService;
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
	public long generateReviewAsync(Product product, VerticalConfig verticalConfig,
			CompletableFuture<Void> preProcessingFuture) {
		long upc = product.getId();
		if (!shouldGenerateReview(product)) {
			logger.info(
					"Skipping asynchronous AI review generation for UPC {} because an up-to-date review already exists.",
					upc);
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
					promptVariables = preprocessingService.preparePromptVariables(product, verticalConfig, status);
				}
				status.addEvent(ReviewGenerationStatus.ProgressEventType.STARTED, "AI generation started", null);
				PromptResponse<AiReview> reviewResponse;
				if (promptConfig.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH) {
					reviewResponse = genAiService.objectPromptStream(promptKey, promptVariables, AiReview.class,
							event -> handleProviderEvent(status, event));
				} else {
					reviewResponse = genAiService.objectPrompt(promptKey, promptVariables, AiReview.class);
				}
				AiReview newReview = applyCitationsAndNormalize(reviewResponse.getBody(), reviewResponse.getMetadata());
				newReview = updateAiReviewReferences(newReview);

				// Populate attributes
				populateAttributes(product, newReview);

				holder.setReview(newReview);
				holder.setSources((Map<String, Integer>) promptVariables.get("SOURCE_TOKENS"));
				holder.setTotalTokens((Integer) promptVariables.get("TOTAL_TOKENS"));
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
			} catch (Exception e) {
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
	 * Submits a batch review generation request.
	 * <p>
	 * For each eligible product, it prepares prompt variables (including Google
	 * searches, URL fetching, token counting, etc.) and collects the product GTINs.
	 * Then it submits a batch job via the BatchPromptService using the prompt key
	 * "review-generation". Instead of using a future callback, a tracking file is
	 * written to a designated folder so that a scheduled task can later check job
	 * status and process the results.
	 * </p>
	 *
	 * @param products       the list of products to process.
	 * @param verticalConfig the vertical configuration.
	 * @return the batch job ID.
	 */
	public String generateReviewBatchRequest(List<Product> products, VerticalConfig verticalConfig) {
		List<Map<String, Object>> promptVariablesList = new ArrayList<>();
		List<String> ids = new ArrayList<>();
		String promptKey = resolvePromptKey();
		PromptConfig promptConfig = genAiService.getPromptConfig(promptKey);
		if (promptConfig != null && promptConfig.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH) {
			throw new IllegalStateException("Batch review generation is not supported for model-native search.");
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
				promptVariablesList.add(preprocessingService.preparePromptVariables(product, verticalConfig, status));
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
		// Submit batch job using the updated batchPromptRequest method in
		// BatchPromptService.
		String jobId = batchAiService.batchPromptRequest(promptKey, promptVariablesList, ids, AiReview.class);
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
	 * complete), and if the job is complete, processes each BatchOutput to update
	 * the corresponding product review. Finally, the tracking file is deleted.
	 * TODO(p2,design) : separate the handle method
	 * </p>
	 */
	public void checkBatchJobStatuses() {
		File[] trackingFiles = trackingFolder
				.listFiles((dir, name) -> name.startsWith("tracking_") && name.endsWith(".json"));
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
					handleBatchResponse(jobId, response);
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
			handleBatchResponse(jobId, response);
		}
	}

	private void handleBatchResponse(String jobId, PromptResponse<List<BatchOutput>> response)
			throws ResourceNotFoundException, IOException {
		logger.info("Batch job {} completed. Processing {} outputs.", jobId, response.getBody().size());
		// For each batch output (assumed to contain a customId that corresponds to the
		// product GTIN)
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
	 *
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
				// TODO: Deduct provider name from source or config
				source = AI_SOURCE_NAME;
				agg.addSourceAttribute(new SourcedAttribute(new Attribute(a.getName(), a.getValue(), "fr"), source));

				// Replacing new AggAttribute in product
				product.getAttributes().getAll().put(agg.getName(), agg);
			} catch (Exception e1) {
				logger.error("Cannot extract domain name", e1);
			}

		});
	}

	// -------------------- Helper Methods -------------------- //


	private AiReview processBatchOutputToAiReview(BatchOutput output) {
		if (output.response().body().choices().size() > 1) {
			logger.error("Error, multiple choices for {}", output);
		}
		// TODO(p2, perf) : instance variable
		var outputConverter = new BeanOutputConverter<>(AiReview.class);

		String jsonContent = output.response().body().choices().getFirst().message().getContent();
		AiReview ret = outputConverter.convert(jsonContent);

		ret = updateAiReviewReferences(ret);

//			AiReview ret = serialisationService.fromJson(jsonContent, AiReview.class);
		return ret;
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
		String description = replaceReferences(review.getDescription());
		String shortDescription = replaceReferences(review.getShortDescription());
		String mediumTitle = replaceReferences(review.getMediumTitle());
		String shortTitle = replaceReferences(review.getShortTitle());
		String technicalReview = replaceReferences(review.getTechnicalReview());
		String ecologicalReview = replaceReferences(review.getEcologicalReview());
		String summary = replaceReferences(review.getSummary());
		String dataQuality = replaceReferences(review.getDataQuality());
		List<String> pros = review.getPros() == null
				? List.of()
				: review.getPros().stream().map(this::replaceReferences).toList();
		List<String> cons = review.getCons() == null
				? List.of()
				: review.getCons().stream().map(this::replaceReferences).toList();
		List<AiReview.AiSource> sources = review.getSources() == null
				? List.of()
				: review.getSources().stream()
				.map(source -> new AiReview.AiSource(source.getNumber(), replaceReferences(source.getName()),
						replaceReferences(source.getDescription()), source.getUrl()))
				.toList();
		List<AiReview.AiAttribute> attributes = review.getAttributes() == null
				? List.of()
				: review.getAttributes().stream()
				.map(attr -> new AiReview.AiAttribute(replaceReferences(attr.getName()),
						replaceReferences(attr.getValue()), attr.getNumber()))
				.toList();
		return new AiReview(description, shortDescription, mediumTitle, shortTitle, technicalReview, ecologicalReview,
				summary, pros, cons, sources, attributes, dataQuality);
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
		if (review == null) {
			return null;
		}
		List<AiReview.AiSource> sources = review.getSources() == null ? List.of() : review.getSources();
		List<AiReview.AiSource> metadataSources = resolveSourcesFromMetadata(metadata);
		if (!metadataSources.isEmpty()) {
			sources = metadataSources;
		}
		int maxSourceNumber = sources == null ? 0 : sources.size();
		ReferenceNormalization normalization = normalizeReferences(review, sources, maxSourceNumber);
		String dataQuality = normalization.dataQuality();
		return new AiReview(normalization.description(), normalization.shortDescription(), normalization.mediumTitle(),
				normalization.shortTitle(), normalization.technicalReview(), normalization.ecologicalReview(),
				normalization.summary(), normalization.pros(), normalization.cons(), sources, normalization.attributes(),
				dataQuality);
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
		String technicalReview = normalizer.normalize(review.getTechnicalReview());
		String ecologicalReview = normalizer.normalize(review.getEcologicalReview());
		String summary = normalizer.normalize(review.getSummary());
		String dataQuality = review.getDataQuality();
		List<String> pros = review.getPros() == null
				? List.of()
				: review.getPros().stream().map(normalizer::normalize).toList();
		List<String> cons = review.getCons() == null
				? List.of()
				: review.getCons().stream().map(normalizer::normalize).toList();
		List<AiReview.AiAttribute> attributes = review.getAttributes() == null
				? List.of()
				: review.getAttributes().stream()
				.map(attr -> new AiReview.AiAttribute(normalizer.normalize(attr.getName()),
						normalizer.normalize(attr.getValue()), attr.getNumber()))
				.toList();

		if (maxSourceNumber == 0) {
			dataQuality = appendDataQuality(dataQuality, "Aucune source fiable n'a été trouvée.");
		} else if (normalizer.hasRemovedReferences()) {
			dataQuality = appendDataQuality(dataQuality,
					"Certaines références ont été retirées car elles ne correspondaient à aucune source.");
		}
		return new ReferenceNormalization(description, shortDescription, mediumTitle, shortTitle, technicalReview,
				ecologicalReview, summary, pros, cons, attributes, dataQuality);
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
			String shortTitle, String technicalReview, String ecologicalReview, String summary, List<String> pros,
			List<String> cons, List<AiReview.AiAttribute> attributes, String dataQuality) {
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

}
