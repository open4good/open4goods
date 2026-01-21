package org.open4goods.services.reviewgeneration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.Localisable;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.exceptions.BatchJobFailedException;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class ReviewGenerationServiceTest {

    private ReviewGenerationService reviewGenerationService;

    private ReviewGenerationConfig properties;
    @Mock private GoogleSearchService googleSearchService;
    @Mock private UrlFetchingService urlFetchingService;
    @Mock private PromptService genAiService;
    @Mock private BatchPromptService batchAiService;
    @Mock private ProductRepository productRepository;
    @Mock private ReviewGenerationPreprocessingService preprocessingService;
    @Mock private VerticalsConfigService verticalsConfigService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        properties = new ReviewGenerationConfig();
        properties.setRegenerationDelayDays(30);
        properties.setRetryDelayDays(7);
        properties.setBatchFolder(System.getProperty("java.io.tmpdir") + "/batch-test");

        meterRegistry = new SimpleMeterRegistry();

        reviewGenerationService = new ReviewGenerationService(
                properties,
                googleSearchService,
                urlFetchingService,
                genAiService,
                batchAiService,
                meterRegistry,
                productRepository,
                preprocessingService,
                verticalsConfigService
        );
    }



    @Test
    void shouldGenerateReview_ReturnsTrue_WhenNoReviewExists() {
        Product product = new Product();
        product.setReviews(new Localisable<>()); // Empty reviews

        boolean result = invokeShouldGenerateReview(product);

        assertThat(result).isTrue();
    }

    @Test
    void shouldGenerateReview_ReturnsTrue_WhenReviewIsOutdated() {
        Product product = new Product();
        Localisable<String, AiReviewHolder> reviews = new Localisable<>();
        AiReviewHolder holder = new AiReviewHolder();
        holder.setEnoughData(true);
        // Created 31 days ago (limit is 30)
        holder.setCreatedMs(Instant.now().minus(31, ChronoUnit.DAYS).toEpochMilli());
        reviews.put("fr", holder);
        product.setReviews(reviews);

        boolean result = invokeShouldGenerateReview(product);

        assertThat(result).isTrue();
    }

    @Test
    void shouldGenerateReview_ReturnsFalse_WhenReviewIsFresh() {
        Product product = new Product();
        Localisable<String, AiReviewHolder> reviews = new Localisable<>();
        AiReviewHolder holder = new AiReviewHolder();
        holder.setEnoughData(true);
        // Created 1 day ago
        holder.setCreatedMs(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());

        // Ensure the review is valid
        AiReview review = new AiReview();
        review.setDescription("This is a sufficiently long description that should pass the validation check of 20 characters.");
        review.setAttributes(java.util.List.of(new AiReview.AiAttribute("attr1", "val1", 1)));
        holder.setReview(review);

        reviews.put("fr", holder);
        product.setReviews(reviews);

        boolean result = invokeShouldGenerateReview(product);

        assertThat(result).isFalse();
    }

    @Test
    void shouldGenerateReview_ReturnsTrue_WhenReviewIsInvalid_ShortDescription() {
        Product product = new Product();
        Localisable<String, AiReviewHolder> reviews = new Localisable<>();
        AiReviewHolder holder = new AiReviewHolder();
        holder.setEnoughData(true);
        holder.setCreatedMs(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());

        AiReview review = new AiReview();
        review.setDescription("Too short"); // < 20 chars
        review.setAttributes(java.util.List.of(new AiReview.AiAttribute("attr1", "val1", 1)));
        holder.setReview(review);

        reviews.put("fr", holder);
        product.setReviews(reviews);

        boolean result = invokeShouldGenerateReview(product);

        assertThat(result).isTrue();
    }

    @Test
    void shouldGenerateReview_ReturnsTrue_WhenReviewIsInvalid_NoAttributes() {
        Product product = new Product();
        Localisable<String, AiReviewHolder> reviews = new Localisable<>();
        AiReviewHolder holder = new AiReviewHolder();
        holder.setEnoughData(true);
        holder.setCreatedMs(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());

        AiReview review = new AiReview();
        review.setDescription("This is a sufficiently long description that should pass the validation check.");
        review.setAttributes(java.util.Collections.emptyList()); // Empty attributes
        holder.setReview(review);

        reviews.put("fr", holder);
        product.setReviews(reviews);

        boolean result = invokeShouldGenerateReview(product);

        assertThat(result).isTrue();
    }



    // Helper to invoke private method
    private boolean invokeShouldGenerateReview(Product product) {
        return ReflectionTestUtils.invokeMethod(reviewGenerationService, "shouldGenerateReview", product);
    }

    @Test
    void testGenerateReviewAsync_MissingVariable_ShouldFail() throws Exception {
        // Setup
        Product product = new Product();
        product.setId(123L);
        // Create a basic gtin to avoid NPE in isActiveForGtin or other checks if accessed
        // Product uses id for upc, and gtin() usually returns something or null
        // Let's assume default is null, but the service might access it.
        // We will assert failure anyway.

        org.open4goods.model.vertical.VerticalConfig verticalConfig = new org.open4goods.model.vertical.VerticalConfig();

        org.open4goods.services.prompt.config.PromptConfig promptConfig = new org.open4goods.services.prompt.config.PromptConfig();
        promptConfig.setRetrievalMode(org.open4goods.services.prompt.config.RetrievalMode.EXTERNAL_SOURCES);
        org.mockito.Mockito.when(genAiService.getPromptConfig(org.mockito.ArgumentMatchers.anyString())).thenReturn(promptConfig);

        // Mock preprocessing to return incomplete variables
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("PRODUCT_BRAND", "Brand");
        // Missing PRODUCT_MODEL, VERTICAL_NAME, etc.
        org.mockito.Mockito.when(preprocessingService.preparePromptVariables(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(variables);

        // Execute
        reviewGenerationService.generateReviewAsync(product, verticalConfig, null, true);

        // Wait for execution (1s should be enough for local thread pool)
        long start = System.currentTimeMillis();
        org.open4goods.model.review.ReviewGenerationStatus status = null;
        while (System.currentTimeMillis() - start < 2000) {
             status = reviewGenerationService.getProcessStatus(123L);
             if (status != null && (status.getStatus() == org.open4goods.model.review.ReviewGenerationStatus.Status.FAILED || status.getStatus() == org.open4goods.model.review.ReviewGenerationStatus.Status.SUCCESS)) {
                 break;
             }
             Thread.sleep(100);
        }

        // Check status
        if (status == null) {
            throw new RuntimeException("Status is null");
        }

        if (status.getStatus() != org.open4goods.model.review.ReviewGenerationStatus.Status.FAILED) {
             throw new RuntimeException("Expected FAILED status but got " + status.getStatus() + ". Error: " + status.getErrorMessage());
        }

        if (!status.getErrorMessage().contains("Missing required prompt variable")) {
            throw new RuntimeException("Expected error message to contain 'Missing required prompt variable', but got: " + status.getErrorMessage());
        }
    }

    @Test
    void shouldGenerateReviewBatch_WithGrounding_DoesNotThrowException() throws Exception {
        // Setup
        Product product = new Product();
        product.setId(1234567890123L);
        // product.setGtin("1234567890123"); // setGtin does not exist, ID is used
        product.setReviews(new Localisable<>()); // No review so shouldGenerate returns true

        java.util.List<Product> products = java.util.List.of(product);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = new org.open4goods.model.vertical.VerticalConfig();
        verticalConfig.setId("tv");

        // Mock Prompt Config with MODEL_WEB_SEARCH
        org.open4goods.services.prompt.config.PromptConfig promptConfig = new org.open4goods.services.prompt.config.PromptConfig();
        promptConfig.setRetrievalMode(org.open4goods.services.prompt.config.RetrievalMode.MODEL_WEB_SEARCH);
        org.mockito.Mockito.when(genAiService.getPromptConfig(org.mockito.ArgumentMatchers.any())).thenReturn(promptConfig);

        // Mock preprocessing.buildBasePromptVariables
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("VAR", "VALUE");
        org.mockito.Mockito.when(preprocessingService.buildBasePromptVariables(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(variables);

        // Mock BatchPromptService to return a dummy job ID
        org.mockito.Mockito.when(batchAiService.batchPromptRequest(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn("job-123");

        // Execute
        String jobId = reviewGenerationService.generateReviewBatchRequest(products, verticalConfig);

        // Verify
        assertThat(jobId).isEqualTo("job-123");
        // Verify that buildBasePromptVariables was called (and not preparePromptVariables)
        org.mockito.Mockito.verify(preprocessingService).buildBasePromptVariables(org.mockito.ArgumentMatchers.eq(product), org.mockito.ArgumentMatchers.eq(verticalConfig));
        org.mockito.Mockito.verify(preprocessingService, org.mockito.Mockito.never()).preparePromptVariables(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loadNextTopImpactScoreProducts_FiltersExistingReviews() {
        Product withReview = new Product();
        withReview.setId(1L);
        Localisable<String, AiReviewHolder> reviews = new Localisable<>();
        AiReviewHolder holder = new AiReviewHolder();
        holder.setReview(new AiReview());
        reviews.put("fr", holder);
        withReview.setReviews(reviews);

        Product withoutReview = new Product();
        withoutReview.setId(2L);
        withoutReview.setReviews(new Localisable<>());

        when(productRepository.exportVerticalWithValidDateAndMissingReviewOrderByImpactScore(eq("tv"), eq("fr"), anyInt(), eq(false)))
                .thenReturn(Stream.of(withoutReview));

        org.open4goods.model.vertical.VerticalConfig verticalConfig = new org.open4goods.model.vertical.VerticalConfig();
        verticalConfig.setId("tv");

        List<Product> results = ReflectionTestUtils.invokeMethod(
                reviewGenerationService,
                "loadNextTopImpactScoreProducts",
                verticalConfig,
                1
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void handleTrackingFile_MarksFailureWhenBatchJobFails() throws Exception {
        Path trackingDir = Path.of(properties.getBatchFolder(), "tracking");
        Files.createDirectories(trackingDir);
        Path trackingFile = trackingDir.resolve("tracking_test.json");
        Map<String, Object> trackingInfo = Map.of(
                "jobId", "job-123",
                "productIds", List.of("10", "11"),
                "gtins", List.of("gtin-10", "gtin-11"),
                "verticalId", "tv",
                "createdAt", Instant.now().toEpochMilli()
        );
        Files.writeString(trackingFile, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(trackingInfo));

        when(batchAiService.batchPromptResponse("job-123"))
                .thenThrow(new BatchJobFailedException("provider failure"));

        ReflectionTestUtils.invokeMethod(reviewGenerationService, "handleTrackingFile", trackingFile.toFile());

        assertThat(reviewGenerationService.getProcessStatus(10L).getStatus())
                .isEqualTo(org.open4goods.model.review.ReviewGenerationStatus.Status.FAILED);
        assertThat(reviewGenerationService.getProcessStatus(11L).getStatus())
                .isEqualTo(org.open4goods.model.review.ReviewGenerationStatus.Status.FAILED);
        assertThat(reviewGenerationService.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(Files.exists(trackingFile)).isFalse();
    }
}
