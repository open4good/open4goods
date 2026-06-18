package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductFetchDiagnostics;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationVerticalResult;
import org.open4goods.services.reviewgeneration.dto.SourceDiscoveryJob;
import org.open4goods.services.reviewgeneration.service.DataForSeoSerpService;
import org.open4goods.services.reviewgeneration.service.NotEnoughDataException;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing AI review generation endpoints for the back-office API.
 * <p>
 * The controller mirrors the behaviour previously offered by the legacy UI module: a POST endpoint triggers
 * the asynchronous generation process while a GET endpoint returns the latest status snapshot for polling clients.
 * API access is guarded by the {@link RolesConstants#ROLE_ADMIN} authority which is granted by the API key interceptor.
 * </p>
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Review generation", description = "Trigger and inspect AI review generation jobs.")
public class ReviewGenerationController {

    private final ProductRepository productRepository;
    private final VerticalsConfigService verticalsConfigService;
    private final ReviewGenerationService reviewGenerationService;
    private final DataForSeoSerpService dataForSeoSerpService;

    public ReviewGenerationController(ProductRepository productRepository,
            VerticalsConfigService verticalsConfigService,
            ReviewGenerationService reviewGenerationService,
            DataForSeoSerpService dataForSeoSerpService) {
        this.productRepository = productRepository;
        this.verticalsConfigService = verticalsConfigService;
        this.reviewGenerationService = reviewGenerationService;
        this.dataForSeoSerpService = dataForSeoSerpService;
    }

    /**
     * Trigger asynchronous AI review generation for the requested product.
     *
     * @param upc the product identifier used as review generation key
     * @return the UPC echoed back once the generation has been scheduled
     * @throws ResourceNotFoundException when the product does not exist in the repository
     */

    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/{id}")
    @Operation(summary = "Schedule AI review generation", description = "Launch the asynchronous AI review pipeline for the given UPC.",
            parameters = {
                    @Parameter(name = "id", in = ParameterIn.PATH, required = true,
                            description = "Product UPC used to request review generation.",
                            schema = @Schema(type = "integer", format = "int64", minimum = "0")),

// TODO(P1, security)  : enforce this parameter, tight to an admin role
                    @Parameter(name = "required", in = ParameterIn.PATH, required = false,
                    description = "Force the review generation, even if already processed",
                    schema = @Schema(type = "boolean", format = "bool", minimum = "0", defaultValue = "false")

                    		)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Generation scheduled",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Long.class))),
                    @ApiResponse(responseCode = "404", description = "Product not found")
            })
    public ResponseEntity<Long> generateReview(@PathVariable("id") long upc, @RequestParam(defaultValue = "false") boolean force, HttpServletRequest request) throws ResourceNotFoundException {
        Product product = productRepository.getById(upc);
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(product.getVertical());

        Map<String, String> headers = new HashMap<>();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            headers.put("User-Agent", userAgent);
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            headers.put("X-Forwarded-For", xForwardedFor);
        }
        String xForwardedIp = request.getHeader("X-Forwarded-IP"); // Also requested by user
        if (xForwardedIp != null) {
        	headers.put("X-Forwarded-IP", xForwardedIp);
        }

        long scheduledUpc = reviewGenerationService.generateReviewAsync(product, verticalConfig, CompletableFuture.completedFuture(null), force, headers);
        return ResponseEntity.ok(scheduledUpc);
    }

    /**
     * Retrieve the latest status of an AI review generation request.
     *
     * @param upc the product identifier used to track the process
     * @return the review generation status or {@code null} when no process is tracked for the UPC
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @GetMapping("/review/{id}")
    @Operation(summary = "Get review generation status", description = "Return the latest status snapshot for the requested UPC.",
            parameters = {
                    @Parameter(name = "id", in = ParameterIn.PATH, required = true,
                            description = "Product UPC used when the review generation was requested.",
                            schema = @Schema(type = "integer", format = "int64", minimum = "0"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ReviewGenerationStatus.class))),
                    @ApiResponse(responseCode = "404", description = "Product not found", content = @Content())
            })
    public ResponseEntity<ReviewGenerationStatus> getReviewStatus(@PathVariable("id") long upc) {
        ReviewGenerationStatus status = reviewGenerationService.getProcessStatus(upc);
        return ResponseEntity.ok(status);
    }

    /**
     * Retrieve the resolved prompt for review generation without executing the AI call.
     *
     * @param upc the product identifier
     * @return the resolved prompt configuration
     * @throws ResourceNotFoundException when the product does not exist
     * @throws Exception when prompt generation fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @GetMapping("/review/{id}/prompt")
    @Operation(summary = "Get review generation prompt", description = "Return the fully resolved prompt configuration (dry run) for the requested UPC.",
            parameters = {
                    @Parameter(name = "id", in = ParameterIn.PATH, required = true,
                            description = "Product UPC.",
                            schema = @Schema(type = "integer", format = "int64", minimum = "0"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Prompt returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = org.open4goods.services.prompt.config.PromptConfig.class))),
                    @ApiResponse(responseCode = "404", description = "Product not found")
            })
    public ResponseEntity<org.open4goods.services.prompt.config.PromptConfig> getReviewPrompt(@PathVariable("id") long upc) throws Exception {
        Product product = productRepository.getById(upc);
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(product.getVertical());
        org.open4goods.services.prompt.config.PromptConfig promptConfig = reviewGenerationService.generateReviewDryRun(product, verticalConfig);
        return ResponseEntity.ok(promptConfig);
    }

    @PostMapping("/enrichment/{id}/urls/discover")
    @Operation(summary = "Discover enrichment source URLs for a product",
            description = "Submit a DataForSEO Standard SERP task for source URL discovery.")
    public ResponseEntity<SourceDiscoveryJob> discoverEnrichmentUrls(@PathVariable("id") long upc,
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception {
        Product product = productRepository.getById(upc);
        return ResponseEntity.ok(dataForSeoSerpService.discoverUrls(product, force));
    }

    @PostMapping("/enrichment/vertical/{verticalId}/urls/discover")
    @Operation(summary = "Discover enrichment source URLs for a vertical",
            description = "Submit DataForSEO Standard SERP tasks in chunks of up to 100 products.")
    public ResponseEntity<SourceDiscoveryJob> discoverEnrichmentUrlsForVertical(
            @PathVariable String verticalId,
            @RequestParam(value = "limit", defaultValue = "100") int limit,
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception {
        return ResponseEntity.ok(dataForSeoSerpService.discoverUrlsForVertical(verticalId, limit, force));
    }

    @PostMapping("/enrichment/discovery/jobs/{jobId}/poll")
    @Operation(summary = "Poll an enrichment URL discovery job")
    public ResponseEntity<SourceDiscoveryJob> pollEnrichmentDiscoveryJob(@PathVariable String jobId) throws Exception {
        return ResponseEntity.ok(dataForSeoSerpService.pollJob(jobId));
    }

    @GetMapping("/enrichment/discovery/jobs/{jobId}")
    @Operation(summary = "Get an enrichment URL discovery job")
    public ResponseEntity<SourceDiscoveryJob> getEnrichmentDiscoveryJob(@PathVariable String jobId) throws Exception {
        return ResponseEntity.ok(dataForSeoSerpService.getJob(jobId));
    }

    /**
     * Run only the remote fetching stage for one product.
     *
     * @param upc product UPC
     * @param request HTTP request carrying fetch headers
     * @return persisted fetch-stage result
     * @throws Exception when fetching fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/{id}/fetch")
    @Operation(summary = "Run review remote fetching for a product",
            description = "Fetch and persist review markdown sources without running LLM completions.")
    public ResponseEntity<ReviewGenerationStepResult> fetchReviewSources(@PathVariable("id") long upc,
            HttpServletRequest request) throws Exception {
        Product product = productRepository.getById(upc);
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(product.getVertical());
        try {
            return ResponseEntity.ok(reviewGenerationService.fetchReviewSources(product, verticalConfig,
                    requestHeaders(request)));
        } catch (NotEnoughDataException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(failureResult(product, verticalConfig, "fetch", e));
        }
    }

    /**
     * Run only the attribute extraction stage for one product.
     *
     * @param upc product UPC
     * @return persisted attribute-stage result
     * @throws Exception when extraction fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/{id}/attributes")
    @Operation(summary = "Run review attribute extraction for a product",
            description = "Use persisted reviewFacts to extract and persist product attributes.")
    public ResponseEntity<ReviewGenerationStepResult> extractReviewAttributes(@PathVariable("id") long upc)
            throws Exception {
        Product product = productRepository.getById(upc);
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(product.getVertical());
        try {
            return ResponseEntity.ok(reviewGenerationService.extractReviewAttributes(product, verticalConfig));
        } catch (NotEnoughDataException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(failureResult(product, verticalConfig, "attributes", e));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(failureResult(product, verticalConfig, "attributes", e));
        }
    }

    /**
     * Run only the text completion stage for one product.
     *
     * @param upc product UPC
     * @return persisted text-stage result
     * @throws Exception when text generation fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/{id}/text")
    @Operation(summary = "Run review text completion for a product",
            description = "Use persisted reviewFacts and product attributes to generate and persist the AI review.")
    public ResponseEntity<ReviewGenerationStepResult> generateReviewText(@PathVariable("id") long upc)
            throws Exception {
        Product product = productRepository.getById(upc);
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(product.getVertical());
        try {
            return ResponseEntity.ok(reviewGenerationService.generateReviewText(product, verticalConfig));
        } catch (NotEnoughDataException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(failureResult(product, verticalConfig, "text", e));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(failureResult(product, verticalConfig, "text", e));
        }
    }

    /**
     * Run the synchronous fetch, attribute and text workflow for one product.
     *
     * @param upc product UPC
     * @param request HTTP request carrying fetch headers
     * @return persisted text-stage result
     * @throws Exception when any stage fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/{id}/workflow")
    @Operation(summary = "Run full synchronous review workflow for a product",
            description = "Fetch sources, extract attributes, then generate and persist review text.")
    public ResponseEntity<ReviewGenerationStepResult> generateReviewWorkflow(@PathVariable("id") long upc,
            HttpServletRequest request) throws Exception {
        Product product = productRepository.getById(upc);
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(product.getVertical());
        try {
            return ResponseEntity.ok(reviewGenerationService.generateReviewWorkflow(product, verticalConfig,
                    requestHeaders(request)));
        } catch (NotEnoughDataException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(failureResult(product, verticalConfig, "workflow", e));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(failureResult(product, verticalConfig, "workflow", e));
        }
    }

    /**
     * Run only the remote fetching stage for a vertical.
     *
     * @param verticalId vertical identifier
     * @param limit maximum products to process
     * @param request HTTP request carrying fetch headers
     * @return per-product synchronous results
     * @throws IOException when product loading fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/vertical/{verticalId}/fetch")
    @Operation(summary = "Run review remote fetching for a vertical")
    public ResponseEntity<ReviewGenerationVerticalResult> fetchReviewSourcesForVertical(
            @PathVariable String verticalId,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            HttpServletRequest request) throws IOException {
        Map<String, String> headers = requestHeaders(request);
        return ResponseEntity.ok(runVerticalStage(verticalId, limit, "fetch",
                (product, verticalConfig) -> reviewGenerationService.fetchReviewSources(product, verticalConfig,
                        headers)));
    }

    /**
     * Run only the attribute extraction stage for a vertical.
     *
     * @param verticalId vertical identifier
     * @param limit maximum products to process
     * @return per-product synchronous results
     * @throws IOException when product loading fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/vertical/{verticalId}/attributes")
    @Operation(summary = "Run review attribute extraction for a vertical")
    public ResponseEntity<ReviewGenerationVerticalResult> extractReviewAttributesForVertical(
            @PathVariable String verticalId,
            @RequestParam(value = "limit", defaultValue = "5") int limit) throws IOException {
        return ResponseEntity.ok(runVerticalStage(verticalId, limit, "attributes",
                reviewGenerationService::extractReviewAttributes));
    }

    /**
     * Run only the text completion stage for a vertical.
     *
     * @param verticalId vertical identifier
     * @param limit maximum products to process
     * @return per-product synchronous results
     * @throws IOException when product loading fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/vertical/{verticalId}/text")
    @Operation(summary = "Run review text completion for a vertical")
    public ResponseEntity<ReviewGenerationVerticalResult> generateReviewTextForVertical(
            @PathVariable String verticalId,
            @RequestParam(value = "limit", defaultValue = "5") int limit) throws IOException {
        return ResponseEntity.ok(runVerticalStage(verticalId, limit, "text",
                reviewGenerationService::generateReviewText));
    }

    /**
     * Run the synchronous fetch, attribute and text workflow for a vertical.
     *
     * @param verticalId vertical identifier
     * @param limit maximum products to process
     * @param request HTTP request carrying fetch headers
     * @return per-product synchronous results
     * @throws IOException when product loading fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/vertical/{verticalId}/workflow")
    @Operation(summary = "Run full synchronous review workflow for a vertical")
    public ResponseEntity<ReviewGenerationVerticalResult> generateReviewWorkflowForVertical(
            @PathVariable String verticalId,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            HttpServletRequest request) throws IOException {
        Map<String, String> headers = requestHeaders(request);
        return ResponseEntity.ok(runVerticalStage(verticalId, limit, "workflow",
                (product, verticalConfig) -> reviewGenerationService.generateReviewWorkflow(product, verticalConfig,
                        headers)));
    }


    /**
     * Trigger batch review generation for a vertical.
     *
     * @param verticalId the vertical identifier to batch
     * @param top optional limit for the number of products to batch
     * @return the batch job identifier
     * @throws IOException when batch submission fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/batch")
    @Operation(summary = "Schedule AI review generation batch",
            description = "Launch a batch review generation job for a vertical.")
    public ResponseEntity<String> generateReviewBatch(@RequestParam("verticalId") String verticalId,
            @RequestParam(value = "top", required = false) Integer top) throws IOException {
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(verticalId);
        List<Product> products;
        try (Stream<Product> productStream = top == null
                ? productRepository.exportVerticalWithValidDateOrderByEcoscore(verticalId, false)
                : productRepository.exportVerticalWithValidDateOrderByEcoscore(verticalId, top, false)) {
            products = productStream.toList();
        }
        String jobId = reviewGenerationService.generateReviewBatchRequest(products, verticalConfig);
        return ResponseEntity.ok(jobId);
    }

    /**
     * Trigger batch review generation for the next top impact score products.
     *
     * @param verticalId optional vertical identifier; when omitted, all enabled verticals are processed
     * @param limit number of products to include in each batch
     * @return list of batch job identifiers
     * @throws IOException when batch submission fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/batch/impactscore")
    @Operation(summary = "Schedule AI review generation for top impact score products",
            description = "Launch a batch review generation job for the next top impact score products without existing AI reviews.")
    public ResponseEntity<List<String>> generateImpactScoreBatch(
            @RequestParam(value = "verticalId", required = false) String verticalId,
            @RequestParam(value = "limit", defaultValue = "2") int limit,
            @RequestParam(value = "sortOnImpactScore", defaultValue = "true") boolean sortOnImpactScore



    		) throws IOException {
        if (verticalId != null && !verticalId.isBlank()) {
            String jobId = reviewGenerationService.triggerNextTopImpactScoreBatch(verticalId, limit, sortOnImpactScore);
            return ResponseEntity.ok(List.of(jobId));
        }
        List<String> jobIds = reviewGenerationService.triggerNextTopImpactScoreBatches(limit, sortOnImpactScore);
        return ResponseEntity.ok(jobIds);
    }

    /**
     * Trigger batch result handling for a given job identifier.
     *
     * @param jobId the batch job identifier
     * @throws ResourceNotFoundException when the job does not exist
     * @throws IOException when batch response processing fails
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    @PostMapping("/review/batch/process")
    @Operation(summary = "Process batch review generation results",
            description = "Trigger the handling of batch review generation results for a job.")
    public void processBatchResults(@RequestParam("jobId") String jobId)
            throws ResourceNotFoundException, IOException {
        reviewGenerationService.triggerResponseHandling(jobId);
    }

    private ReviewGenerationVerticalResult runVerticalStage(String verticalId, int limit, String step,
            ReviewGenerationStageRunner runner) throws IOException {
        VerticalConfig verticalConfig = verticalsConfigService.getConfigByIdOrDefault(verticalId);
        int effectiveLimit = Math.max(limit, 1);
        List<Product> products;
        try (Stream<Product> productStream = productRepository.exportVerticalWithValidDateOrderByImpactScore(
                verticalId, effectiveLimit, false)) {
            products = productStream.toList();
        }

        List<ReviewGenerationStepResult> results = new ArrayList<>();
        for (Product product : products) {
            try {
                results.add(runner.run(product, verticalConfig));
            } catch (Exception e) {
                if (e instanceof NotEnoughDataException notEnoughDataException) {
                    results.add(failureResult(product, verticalConfig, step, notEnoughDataException));
                    continue;
                }
                results.add(new ReviewGenerationStepResult(product.getId(), product.gtin(), verticalId, step,
                        false, e.getMessage(), product.getReviewFacts() == null ? 0 : product.getReviewFacts().size(),
                        product.getReviewFacts() == null ? 0 : product.getReviewFacts().stream()
                                .filter(fact -> fact != null && fact.getTokenCount() != null)
                                .mapToInt(fact -> fact.getTokenCount())
                                .sum(),
                        List.of(), null));
            }
        }
        int succeeded = (int) results.stream().filter(ReviewGenerationStepResult::success).count();
        return new ReviewGenerationVerticalResult(verticalId, step, effectiveLimit, results.size(), succeeded,
                results.size() - succeeded, results);
    }

    private ReviewGenerationStepResult failureResult(Product product, VerticalConfig verticalConfig, String step,
            NotEnoughDataException exception) {
        int sourceCount = exception.getDetails() == null ? 0 : exception.getDetails().sourceCount();
        int totalTokens = exception.getDetails() == null ? 0 : exception.getDetails().totalTokens();
        return new ReviewGenerationStepResult(product.getId(), product.gtin(),
                verticalConfig == null ? product.getVertical() : verticalConfig.getId(), step, false,
                exception.getMessage(), sourceCount, totalTokens, "FAILED", List.of(), null, exception.getDetails(),
                exception.getDetails() == null ? List.of() : exception.getDetails().searchedQueries(),
                exception.getDetails() == null ? List.of() : exception.getDetails().acceptedUrls(),
                exception.getDetails() == null ? Map.of() : exception.getDetails().rejectedUrls(),
                exception.getEnrichmentStatus());
    }

    private ReviewGenerationStepResult failureResult(Product product, VerticalConfig verticalConfig, String step,
            Exception exception) {
        ProductFetchDiagnostics diagnostics = product.getReviewFetchDiagnostics();
        int sourceCount = diagnostics == null ? product.getReviewFacts() == null ? 0 : product.getReviewFacts().size()
                : diagnostics.getSourceCount();
        int totalTokens = diagnostics == null ? product.getReviewFacts() == null ? 0 : product.getReviewFacts().stream()
                .filter(fact -> fact != null && fact.getTokenCount() != null)
                .mapToInt(fact -> fact.getTokenCount())
                .sum() : diagnostics.getTotalTokens();
        String resultQuality = diagnostics == null ? "FAILED" : diagnostics.getResultQuality();
        List<String> searchedQueries = diagnostics == null ? List.of() : diagnostics.getSearchedQueries();
        List<String> acceptedUrls = diagnostics == null ? product.getReviewFacts() == null ? List.of() : product.getReviewFacts().stream()
                .filter(fact -> fact != null && fact.getUrl() != null && !fact.getUrl().isBlank())
                .map(fact -> fact.getUrl())
                .toList() : diagnostics.getAcceptedUrls();
        Map<String, String> rejectedUrls = diagnostics == null ? Map.of() : diagnostics.getRejectedUrls();
        Map<String, String> enrichmentStatus = diagnostics == null ? Map.of() : diagnostics.getEnrichmentStatus();
        return new ReviewGenerationStepResult(product.getId(), product.gtin(),
                verticalConfig == null ? product.getVertical() : verticalConfig.getId(), step, false,
                "Review generation step failed: " + exception.getMessage(), sourceCount, totalTokens, resultQuality,
                List.of(), null, null, searchedQueries, acceptedUrls, rejectedUrls, enrichmentStatus);
    }

    private Map<String, String> requestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            headers.put("User-Agent", userAgent);
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            headers.put("X-Forwarded-For", xForwardedFor);
        }
        String xForwardedIp = request.getHeader("X-Forwarded-IP");
        if (xForwardedIp != null) {
            headers.put("X-Forwarded-IP", xForwardedIp);
        }
        return headers;
    }

    @FunctionalInterface
    private interface ReviewGenerationStageRunner {
        ReviewGenerationStepResult run(Product product, VerticalConfig verticalConfig) throws Exception;
    }
}
