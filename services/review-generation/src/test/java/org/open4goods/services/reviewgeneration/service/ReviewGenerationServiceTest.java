package org.open4goods.services.reviewgeneration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParserConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.services.googlesearch.exception.GoogleSearchException;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.exceptions.BatchJobFailedException;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.reviewgeneration.dto.AttributeExtractionResult;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.open4goods.verticals.VerticalsConfigService;
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
                (org.open4goods.services.urlfetching.service.UrlFetchingService) urlFetchingService,
                genAiService,
                batchAiService,
                meterRegistry,
                productRepository,
                preprocessingService,
                verticalsConfigService,
                java.util.Collections.emptyList()
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
        AiReview review = validReview();
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

        AiReview review = validReview();
        review.setDescription("Too short"); // < 20 chars
        review.setAttributes(java.util.List.of(new AiReview.AiAttribute("attr1", "val1", 1)));
        holder.setReview(review);

        reviews.put("fr", holder);
        product.setReviews(reviews);

        boolean result = invokeShouldGenerateReview(product);

        assertThat(result).isTrue();
    }

    @Test
    void shouldGenerateReview_ReturnsFalse_WhenFreshReviewHasNoAttributes() {
        Product product = new Product();
        Localisable<String, AiReviewHolder> reviews = new Localisable<>();
        AiReviewHolder holder = new AiReviewHolder();
        holder.setEnoughData(true);
        holder.setCreatedMs(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());

        AiReview review = validReview();
        review.setAttributes(java.util.Collections.emptyList()); // Empty attributes
        holder.setReview(review);

        reviews.put("fr", holder);
        product.setReviews(reviews);

        boolean result = invokeShouldGenerateReview(product);

        assertThat(result).isFalse();
    }

    private AiReview validReview() {
        AiReview review = new AiReview();
        review.setDescription("This is a sufficiently long description that should pass the validation check of 20 characters.");
        review.setShortDescription("Short description");
        review.setTechnicalOneline("Technical oneline");
        review.setEcologicalOneline("Ecological oneline");
        review.setCommunityOneline("Community oneline");
        return review;
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
    void shouldGenerateReviewBatch_UsesExternalSources_DoesNotThrowException() throws Exception {
        // Setup
        Product product = new Product();
        product.setId(1234567890123L);
        // product.setGtin("1234567890123"); // setGtin does not exist, ID is used
        product.setReviews(new Localisable<>()); // No review so shouldGenerate returns true

        java.util.List<Product> products = java.util.List.of(product);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = new org.open4goods.model.vertical.VerticalConfig();
        verticalConfig.setId("tv");

        // Grounding has been removed: review generation is always EXTERNAL_SOURCES and the
        // batch path no longer inspects the prompt config retrieval mode.

        // Mock preprocessing.preparePromptVariables (the only retrieval path now)
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("VAR", "VALUE");
        org.mockito.Mockito.when(preprocessingService.preparePromptVariables(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(variables);

        // Mock BatchPromptService to return a dummy job ID
        org.mockito.Mockito.when(batchAiService.batchPromptRequest(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn("job-123");

        // Execute
        String jobId = reviewGenerationService.generateReviewBatchRequest(products, verticalConfig);

        // Verify
        assertThat(jobId).isEqualTo("job-123");
        // Verify that preparePromptVariables was called (and not buildBasePromptVariables)
        org.mockito.Mockito.verify(preprocessingService).preparePromptVariables(org.mockito.ArgumentMatchers.eq(product), org.mockito.ArgumentMatchers.eq(verticalConfig), org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(preprocessingService, org.mockito.Mockito.never()).buildBasePromptVariables(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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

        when(productRepository.exportVerticalWithValidDateAndMissingReviewOrderByImpactScore(
                eq("tv"),
                eq("fr"),
                anyInt(),
                eq(false),
                anyBoolean(),
                eq(properties.getRegenerationDelayDays()),
                eq(properties.getRetryDelayDays())))
                .thenReturn(Stream.of(withoutReview));

        org.open4goods.model.vertical.VerticalConfig verticalConfig = new org.open4goods.model.vertical.VerticalConfig();
        verticalConfig.setId("tv");

        List<Product> results = ReflectionTestUtils.invokeMethod(
                reviewGenerationService,
                "loadNextTopImpactScoreProducts",
                verticalConfig,
                1,
                true
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
        Files.writeString(trackingFile, new tools.jackson.databind.ObjectMapper().writeValueAsString(trackingInfo));

        when(batchAiService.batchPromptResponse("job-123"))
                .thenThrow(new BatchJobFailedException("provider failure"));

        ReflectionTestUtils.invokeMethod(reviewGenerationService, "handleTrackingFile", trackingFile.toFile());

        assertThat(reviewGenerationService.getProcessStatus(10L).getStatus())
                .isEqualTo(org.open4goods.model.review.ReviewGenerationStatus.Status.FAILED);
        assertThat(reviewGenerationService.getProcessStatus(11L).getStatus())
                .isEqualTo(org.open4goods.model.review.ReviewGenerationStatus.Status.FAILED);
        assertThat(reviewGenerationService.health().getStatus()).isEqualTo(org.springframework.boot.health.contributor.Status.DOWN);
        assertThat(Files.exists(trackingFile)).isFalse();
    }

    @Test
    void processAiReview_ShouldApplyNormalizationAndReferences() {
        AiReview review = new AiReview();
        review.setDescription("This is a product — with an em dash [1].");
        review.setShortDescription("Short — [2]");
        review.setPros(List.of("Pro — [1]"));
        review.setCons(List.of("Con — [2]"));
        review.setAttributes(List.of(new AiReview.AiAttribute("Attr —", "Val —", 1)));
        
        // Set new fields
        review.setTechnicalReviewNovice("Tech Novice — [1]");
        review.setTechnicalReviewIntermediate("Tech Intermediate — [2]");
        review.setTechnicalReviewAdvanced("Tech Advanced — [3]");
        review.setEcologicalReviewNovice("Eco Novice — [1]");
        review.setEcologicalReviewIntermediate("Eco Intermediate — [2]");
        review.setEcologicalReviewAdvanced("Eco Advanced — [3]");
        review.setCommunityReviewNovice("Comm Novice — [1]");
        review.setCommunityReviewIntermediate("Comm Intermediate — [2]");
        review.setCommunityReviewAdvanced("Comm Advanced — [3]");
        
        // Disable URL resolution to prevent network calls and filtering during this test
        properties.setResolveUrl(false);

        AiReview.AiSource source1 = new AiReview.AiSource(1, "Source —", "Desc —", "http://example.com/1");
        review.setSources(List.of(source1));
        review.setBaseLine("Base — [1]");
        review.setObsolescenceWarning("Obs — [2]");

        AiReview processed = ReflectionTestUtils.invokeMethod(reviewGenerationService, "processAiReview", review, null);

        assertThat(processed.getDescription()).isEqualTo("This is a product - with an em dash <a class=\"review-ref\" href=\"#review-ref-1\">[1]</a>.");
        assertThat(processed.getShortDescription()).isEqualTo("Short - <a class=\"review-ref\" href=\"#review-ref-2\">[2]</a>");
        assertThat(processed.getPros().get(0)).isEqualTo("Pro - <a class=\"review-ref\" href=\"#review-ref-1\">[1]</a>");
        assertThat(processed.getCons().get(0)).isEqualTo("Con - <a class=\"review-ref\" href=\"#review-ref-2\">[2]</a>");
        assertThat(processed.getAttributes().get(0).getName()).isEqualTo("Attr -");
        assertThat(processed.getAttributes().get(0).getValue()).isEqualTo("Val -");
        assertThat(processed.getSources().get(0).getName()).isEqualTo("Source -");
        assertThat(processed.getSources().get(0).getDescription()).isEqualTo("Desc -");
        
        // Assert new fields
        assertThat(processed.getBaseLine()).isEqualTo("Base - <a class=\"review-ref\" href=\"#review-ref-1\">[1]</a>");
        assertThat(processed.getObsolescenceWarning()).isEqualTo("Obs - <a class=\"review-ref\" href=\"#review-ref-2\">[2]</a>");
        assertThat(processed.getTechnicalReviewNovice()).isEqualTo("Tech Novice - <a class=\"review-ref\" href=\"#review-ref-1\">[1]</a>");
        assertThat(processed.getTechnicalReviewIntermediate()).isEqualTo("Tech Intermediate - <a class=\"review-ref\" href=\"#review-ref-2\">[2]</a>");
        assertThat(processed.getTechnicalReviewAdvanced()).isEqualTo("Tech Advanced - <a class=\"review-ref\" href=\"#review-ref-3\">[3]</a>");
        assertThat(processed.getEcologicalReviewNovice()).isEqualTo("Eco Novice - <a class=\"review-ref\" href=\"#review-ref-1\">[1]</a>");
        assertThat(processed.getEcologicalReviewIntermediate()).isEqualTo("Eco Intermediate - <a class=\"review-ref\" href=\"#review-ref-2\">[2]</a>");
        assertThat(processed.getEcologicalReviewAdvanced()).isEqualTo("Eco Advanced - <a class=\"review-ref\" href=\"#review-ref-3\">[3]</a>");
        assertThat(processed.getCommunityReviewNovice()).isEqualTo("Comm Novice - <a class=\"review-ref\" href=\"#review-ref-1\">[1]</a>");
        assertThat(processed.getCommunityReviewIntermediate()).isEqualTo("Comm Intermediate - <a class=\"review-ref\" href=\"#review-ref-2\">[2]</a>");
        assertThat(processed.getCommunityReviewAdvanced()).isEqualTo("Comm Advanced - <a class=\"review-ref\" href=\"#review-ref-3\">[3]</a>");
    }
    @Test
    void shouldFilterExcludedDomains() {
        // Enable URL resolution
        properties.setResolveUrl(true);
        properties.setExcludedDomains(List.of("excluded.com"));

        AiReview review = new AiReview();
        review.setDescription("Desc [1]");
        // This URL should be filtered out
        AiReview.AiSource source1 = new AiReview.AiSource(1, "Excluded Source", "Desc", "http://excluded.com/resource");
        review.setSources(List.of(source1));

        AiReview processed = ReflectionTestUtils.invokeMethod(reviewGenerationService, "processAiReview", review, null);

        assertThat(processed.getSources()).isEmpty();
    }

    @Test
    void processAiReview_ShouldMergeGroundingSources() {
        // Setup
        properties.setResolveUrl(false);
        AiReview review = new AiReview();
        review.setDescription("Based on search [1] and [2].");
        
        // AI generated source (Initially #1)
        AiReview.AiSource aiSource = new AiReview.AiSource(1, "AI Source", "From AI", "http://ai-generated.com");
        review.setSources(List.of(aiSource));

        // Grounding Metadata
        Map<String, Object> webMetadata = Map.of(
            "uri", "http://grounding-source.com",
            "title", "Grounding Title"
        );
        Map<String, Object> chunk = Map.of("web", webMetadata);
        Map<String, Object> groundingMetadata = Map.of("groundingChunks", List.of(chunk));
        Map<String, Object> metadata = Map.of("groundingMetadata", groundingMetadata);

        // Execute
        AiReview processed = ReflectionTestUtils.invokeMethod(reviewGenerationService, "processAiReview", review, metadata);

        // Assert
        assertThat(processed.getSources()).hasSize(2);
        
        // Grounding source should be first (index 1)
        assertThat(processed.getSources().get(0).getUrl()).isEqualTo("http://grounding-source.com");
        assertThat(processed.getSources().get(0).getName()).isEqualTo("Grounding Title");
        assertThat(processed.getSources().get(0).getNumber()).isEqualTo(1);
        
        // AI source should be second (index 2)
        assertThat(processed.getSources().get(1).getUrl()).isEqualTo("http://ai-generated.com");
        assertThat(processed.getSources().get(1).getNumber()).isEqualTo(2);

        // Text references should be normalized
        // [1] becomes [1] (pointing to Grounding Source)
        // [2] becomes [2] (pointing to AI Source)
        assertThat(processed.getDescription()).contains("<a class=\"review-ref\" href=\"#review-ref-1\">[1]</a>");
        assertThat(processed.getDescription()).contains("<a class=\"review-ref\" href=\"#review-ref-2\">[2]</a>");
    }

    @Test
    void extractReviewAttributes_ShouldRejectUnknownKeysAndInvalidSourceNumbers() throws Exception {
        Product product = new Product();
        product.setId(100L);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = verticalConfig("DIAGONALE_POUCES");
        Map<String, Object> variables = Map.of("ACCEPTED_URLS", List.of("https://www.samsung.com/tv"));
        when(preprocessingService.buildPromptVariablesFromReviewFacts(product, verticalConfig, true))
                .thenReturn(new HashMap<>(variables));
        when(genAiService.objectPrompt(eq(properties.getAttributeExtractionPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AttributeExtractionResult.class)))
                .thenReturn(attributeResponse(List.of(
                        new AiReview.AiAttribute("DIAGONALE_POUCES", "55", 1),
                        new AiReview.AiAttribute("UNKNOWN", "x", 1),
                        new AiReview.AiAttribute("DIAGONALE_POUCES", "65", 2),
                        new AiReview.AiAttribute("DIAGONALE_POUCES", "75", null))));

        org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult result =
                reviewGenerationService.extractReviewAttributes(product, verticalConfig);

        assertThat(result.attributes()).extracting(AiReview.AiAttribute::getName)
                .containsExactly("DIAGONALE_POUCES");
        assertThat(product.getAttributes().getAll()).containsOnlyKeys("DIAGONALE_POUCES");
        assertThat(product.getAttributes().getAll().get("DIAGONALE_POUCES").getSource())
                .extracting(SourcedAttribute::getDataSourcename)
                .containsExactly("AI_REVIEW:s01:www.samsung.com");
    }

    @Test
    void extractReviewAttributes_ShouldRemovePreviousAiSourcesAndPreserveNonAiSourcesOnRerun() throws Exception {
        Product product = new Product();
        product.setId(101L);
        ProductAttribute attribute = new ProductAttribute();
        attribute.setName("DIAGONALE_POUCES");
        attribute.addSourceAttribute(new SourcedAttribute(new Attribute("DIAGONALE_POUCES", "old", "fr"),
                "AI_REVIEW:s01:old.example"));
        attribute.addSourceAttribute(new SourcedAttribute(new Attribute("DIAGONALE_POUCES", "legacy", "fr"), "gpt-4o-mini"));
        attribute.addSourceAttribute(new SourcedAttribute(new Attribute("DIAGONALE_POUCES", "55", "fr"), "EPREL"));
        product.getAttributes().getAll().put("DIAGONALE_POUCES", attribute);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = verticalConfig("DIAGONALE_POUCES");
        when(preprocessingService.buildPromptVariablesFromReviewFacts(product, verticalConfig, true))
                .thenReturn(new HashMap<>(Map.of("ACCEPTED_URLS", List.of("https://support.samsung.com/tv"))));
        when(genAiService.objectPrompt(eq(properties.getAttributeExtractionPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AttributeExtractionResult.class)))
                .thenReturn(attributeResponse(List.of(new AiReview.AiAttribute("DIAGONALE_POUCES", "65", 1))));

        reviewGenerationService.extractReviewAttributes(product, verticalConfig);

        assertThat(product.getAttributes().getAll().get("DIAGONALE_POUCES").getSource())
                .extracting(SourcedAttribute::getDataSourcename)
                .containsExactlyInAnyOrder("EPREL", "AI_REVIEW:s01:support.samsung.com");
    }

    @Test
    void extractReviewAttributes_ShouldReassignSourceNumberWhenValueIsSupportedByAnotherSource() throws Exception {
        Product product = new Product();
        product.setId(103L);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = verticalConfig("NOISE_LEVEL");
        Map<String, Object> variables = new HashMap<>();
        variables.put("ACCEPTED_URLS", List.of("https://manufacturer.example/overview",
                "https://manufacturer.example/specs"));
        variables.put("sources", Map.of(
                "https://manufacturer.example/overview", "Overview without the specific noise value.",
                "https://manufacturer.example/specs", "Technical sheet. Noise level 44 dB."));
        when(preprocessingService.buildPromptVariablesFromReviewFacts(product, verticalConfig, true))
                .thenReturn(variables);
        when(genAiService.objectPrompt(eq(properties.getAttributeExtractionPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AttributeExtractionResult.class)))
                .thenReturn(attributeResponse(List.of(new AiReview.AiAttribute("NOISE_LEVEL", "44", 1))));

        org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult result =
                reviewGenerationService.extractReviewAttributes(product, verticalConfig);

        assertThat(result.attributes()).singleElement()
                .extracting(AiReview.AiAttribute::getNumber)
                .isEqualTo(2);
        assertThat(product.getAttributes().getAll().get("NOISE_LEVEL").getSource())
                .extracting(SourcedAttribute::getDataSourcename)
                .containsExactly("AI_REVIEW:s02:manufacturer.example");
    }

    @Test
    void extractReviewAttributes_ShouldPersistConflictingValuesFromDifferentSources() throws Exception {
        Product product = new Product();
        product.setId(105L);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = verticalConfig("DIAGONALE_POUCES");
        Map<String, Object> variables = new HashMap<>();
        variables.put("ACCEPTED_URLS", List.of("https://manufacturer.example/specs",
                "https://support.example/specs"));
        variables.put("sources", Map.of(
                "https://manufacturer.example/specs", "Technical sheet. Screen size 55 inches.",
                "https://support.example/specs", "Support sheet. Screen size 65 inches."));
        when(preprocessingService.buildPromptVariablesFromReviewFacts(product, verticalConfig, true))
                .thenReturn(variables);
        when(genAiService.objectPrompt(eq(properties.getAttributeExtractionPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AttributeExtractionResult.class)))
                .thenReturn(attributeResponse(List.of(
                        new AiReview.AiAttribute("DIAGONALE_POUCES", "55", 1),
                        new AiReview.AiAttribute("DIAGONALE_POUCES", "65", 2))));

        org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult result =
                reviewGenerationService.extractReviewAttributes(product, verticalConfig);

        assertThat(result.attributes()).hasSize(2);
        assertThat(product.getAttributes().getAll().get("DIAGONALE_POUCES").getSource())
                .extracting(SourcedAttribute::getDataSourcename)
                .containsExactlyInAnyOrder("AI_REVIEW:s01:manufacturer.example", "AI_REVIEW:s02:support.example");
        assertThat(product.getAttributes().getAll().get("DIAGONALE_POUCES").getSource())
                .extracting(SourcedAttribute::getValue)
                .containsExactlyInAnyOrder("55", "65");
    }

    @Test
    void extractReviewAttributes_ShouldNormalizeNumericValuesAndVerifyConvertedSourceUnits() throws Exception {
        Product product = new Product();
        product.setId(106L);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = numericVerticalConfig("HEIGHT", "LENGTH", "cm");
        Map<String, Object> variables = new HashMap<>();
        variables.put("ACCEPTED_URLS", List.of("https://manufacturer.example/specs"));
        variables.put("sources", Map.of("https://manufacturer.example/specs",
                "Technical sheet. Height 420 mm. Width 800 mm."));
        when(preprocessingService.buildPromptVariablesFromReviewFacts(product, verticalConfig, true))
                .thenReturn(variables);
        when(genAiService.objectPrompt(eq(properties.getAttributeExtractionPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AttributeExtractionResult.class)))
                .thenReturn(attributeResponse(List.of(new AiReview.AiAttribute("HEIGHT", "42 cm", 1))));

        org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult result =
                reviewGenerationService.extractReviewAttributes(product, verticalConfig);

        assertThat(result.attributes()).singleElement()
                .extracting(AiReview.AiAttribute::getValue)
                .isEqualTo("42");
        assertThat(product.getAttributes().getAll().get("HEIGHT").getSource())
                .singleElement()
                .extracting(SourcedAttribute::getValue)
                .isEqualTo("42");
    }

    @Test
    void extractReviewAttributes_ShouldRejectAttributeWhenNoAcceptedSourceSupportsValue() throws Exception {
        Product product = new Product();
        product.setId(104L);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = verticalConfig("NOISE_LEVEL");
        Map<String, Object> variables = new HashMap<>();
        variables.put("ACCEPTED_URLS", List.of("https://manufacturer.example/specs"));
        variables.put("sources", Map.of("https://manufacturer.example/specs", "Technical sheet. Noise level 44 dB."));
        when(preprocessingService.buildPromptVariablesFromReviewFacts(product, verticalConfig, true))
                .thenReturn(variables);
        when(genAiService.objectPrompt(eq(properties.getAttributeExtractionPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AttributeExtractionResult.class)))
                .thenReturn(attributeResponse(List.of(new AiReview.AiAttribute("NOISE_LEVEL", "39", 1))));

        org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult result =
                reviewGenerationService.extractReviewAttributes(product, verticalConfig);

        assertThat(result.attributes()).isEmpty();
        assertThat(product.getAttributes().getAll()).doesNotContainKey("NOISE_LEVEL");
    }

    @Test
    void extractReviewAttributes_RestoresAiSourcesRemovedByHooks() throws Exception {
        ReviewGenerationHook destructiveHook = new ReviewGenerationHook() {
            @Override
            public void onReviewGenerated(Product product) {
            }

            @Override
            public void onAttributesExtracted(Product product) {
                product.getAttributes().getAll().clear();
            }
        };
        reviewGenerationService = serviceWithHooks(List.of(destructiveHook));
        Product product = new Product();
        product.setId(107L);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = verticalConfig("DIAGONALE_POUCES");
        when(preprocessingService.buildPromptVariablesFromReviewFacts(product, verticalConfig, true))
                .thenReturn(new HashMap<>(Map.of("ACCEPTED_URLS", List.of("https://manufacturer.example/specs"))));
        when(genAiService.objectPrompt(eq(properties.getAttributeExtractionPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AttributeExtractionResult.class)))
                .thenReturn(attributeResponse(List.of(new AiReview.AiAttribute("DIAGONALE_POUCES", "55", 1))));

        org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult result =
                reviewGenerationService.extractReviewAttributes(product, verticalConfig);

        assertThat(result.attributes()).singleElement()
                .extracting(AiReview.AiAttribute::getName)
                .isEqualTo("DIAGONALE_POUCES");
        assertThat(product.getAttributes().getAll().get("DIAGONALE_POUCES").getSource())
                .extracting(SourcedAttribute::getDataSourcename)
                .containsExactly("AI_REVIEW:s01:manufacturer.example");
    }

    @Test
    void generateReviewText_RestoresAiSourcesRemovedByHooksAndKeepsReviewAttributesEmpty() throws Exception {
        ReviewGenerationHook destructiveHook = new ReviewGenerationHook() {
            @Override
            public void onReviewGenerated(Product product) {
                product.getAttributes().getAll().clear();
            }
        };
        reviewGenerationService = serviceWithHooks(List.of(destructiveHook));
        Product product = new Product();
        product.setId(108L);
        ProductAttribute attribute = new ProductAttribute();
        attribute.setName("DIAGONALE_POUCES");
        attribute.addSourceAttribute(new SourcedAttribute(new Attribute("DIAGONALE_POUCES", "55", "fr"),
                "AI_REVIEW:s01:manufacturer.example"));
        product.getAttributes().getAll().put("DIAGONALE_POUCES", attribute);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = verticalConfig("DIAGONALE_POUCES");
        Map<String, Object> variables = new HashMap<>();
        variables.put("ACCEPTED_URLS", List.of("https://manufacturer.example/specs"));
        variables.put("SOURCE_TOKENS", Map.of("https://manufacturer.example/specs", 100));
        variables.put("TOTAL_TOKENS", 100);
        when(preprocessingService.buildPromptVariablesFromReviewFacts(product, verticalConfig, true))
                .thenReturn(variables);
        AiReview review = new AiReview();
        review.setDescription("A valid review description longer than twenty characters.");
        review.setAttributes(List.of(new AiReview.AiAttribute("DIAGONALE_POUCES", "55", 1)));
        PromptResponse<AiReview> response = new PromptResponse<>();
        response.setBody(review);
        when(genAiService.objectPrompt(eq(properties.getPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AiReview.class))).thenReturn(response);

        org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult result =
                reviewGenerationService.generateReviewText(product, verticalConfig);

        assertThat(result.review().getAttributes()).isEmpty();
        assertThat(product.getReviews().get("fr").getReview().getAttributes()).isEmpty();
        assertThat(product.getAttributes().getAll().get("DIAGONALE_POUCES").getSource())
                .extracting(SourcedAttribute::getDataSourcename)
                .containsExactly("AI_REVIEW:s01:manufacturer.example");
    }

    @Test
    void generateReviewAsync_WithTwoPhaseGeneration_ShouldPersistAttributesBeforeTextGeneration() throws Exception {
        properties.setTwoPhaseGeneration(true);
        Product product = new Product();
        product.setId(102L);
        org.open4goods.model.vertical.VerticalConfig verticalConfig = verticalConfig("DIAGONALE_POUCES");
        org.open4goods.services.prompt.config.PromptConfig promptConfig =
                new org.open4goods.services.prompt.config.PromptConfig();
        promptConfig.setRetrievalMode(org.open4goods.services.prompt.config.RetrievalMode.EXTERNAL_SOURCES);
        when(genAiService.getPromptConfig(org.mockito.ArgumentMatchers.anyString())).thenReturn(promptConfig);
        Map<String, Object> variables = new HashMap<>();
        variables.put("PRODUCT_BRAND", "Samsung");
        variables.put("PRODUCT_MODEL", "S95D");
        variables.put("VERTICAL_NAME", "TV");
        variables.put("OFFER_NAMES", "Samsung S95D");
        variables.put("IMPACTSCORE_POSITION", "Non classe");
        variables.put("COMMON_ATTRIBUTES", "DIAGONALE_POUCES");
        variables.put("ACCEPTED_URLS", List.of("https://www.samsung.com/tv"));
        variables.put("SOURCE_TOKENS", Map.of("https://www.samsung.com/tv", 100));
        variables.put("TOTAL_TOKENS", 100);
        when(preprocessingService.preparePromptVariables(eq(product), eq(verticalConfig),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyMap())).thenReturn(variables);
        when(genAiService.objectPrompt(eq(properties.getAttributeExtractionPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AttributeExtractionResult.class)))
                .thenReturn(attributeResponse(List.of(new AiReview.AiAttribute("DIAGONALE_POUCES", "55", 1))));
        when(genAiService.objectPrompt(eq(properties.getPromptKey()), org.mockito.ArgumentMatchers.anyMap(),
                eq(AiReview.class))).thenAnswer(invocation -> {
                    assertThat(product.getAttributes().getAll()).containsKey("DIAGONALE_POUCES");
                    AiReview review = new AiReview();
                    review.setDescription("A valid review description longer than twenty characters.");
                    review.setAttributes(List.of(new AiReview.AiAttribute("DIAGONALE_POUCES", "55", 1)));
                    PromptResponse<AiReview> response = new PromptResponse<>();
                    response.setBody(review);
                    return response;
                });

        reviewGenerationService.generateReviewAsync(product, verticalConfig, null, true);

        long start = System.currentTimeMillis();
        ReviewGenerationStatus status;
        do {
            status = reviewGenerationService.getProcessStatus(102L);
            Thread.sleep(50);
        } while (System.currentTimeMillis() - start < 2000
                && status.getStatus() != ReviewGenerationStatus.Status.SUCCESS
                && status.getStatus() != ReviewGenerationStatus.Status.FAILED);
        assertThat(status.getStatus()).isEqualTo(ReviewGenerationStatus.Status.SUCCESS);
        assertThat(product.getAttributes().getAll()).containsKey("DIAGONALE_POUCES");
        assertThat(product.getReviews().get("fr").getReview().getAttributes()).isEmpty();
    }

    private PromptResponse<AttributeExtractionResult> attributeResponse(List<AiReview.AiAttribute> attributes) {
        PromptResponse<AttributeExtractionResult> response = new PromptResponse<>();
        response.setBody(new AttributeExtractionResult(attributes));
        return response;
    }

    private ReviewGenerationService serviceWithHooks(List<ReviewGenerationHook> hooks) {
        return new ReviewGenerationService(
                properties,
                googleSearchService,
                (org.open4goods.services.urlfetching.service.UrlFetchingService) urlFetchingService,
                genAiService,
                batchAiService,
                meterRegistry,
                productRepository,
                preprocessingService,
                verticalsConfigService,
                hooks
        );
    }

    private org.open4goods.model.vertical.VerticalConfig verticalConfig(String... keys) {
        org.open4goods.model.vertical.VerticalConfig verticalConfig = new org.open4goods.model.vertical.VerticalConfig();
        verticalConfig.setId("tv");
        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(java.util.Arrays.stream(keys)
                .map(key -> {
                    AttributeConfig config = new AttributeConfig();
                    config.setKey(key);
                    return config;
                })
                .toList());
        verticalConfig.setAttributesConfig(attributesConfig);
        return verticalConfig;
    }

    private org.open4goods.model.vertical.VerticalConfig numericVerticalConfig(String key, String dimension,
            String defaultUnitHint) {
        org.open4goods.model.vertical.VerticalConfig verticalConfig = new org.open4goods.model.vertical.VerticalConfig();
        verticalConfig.setId("tv");
        AttributesConfig attributesConfig = new AttributesConfig();
        AttributeConfig config = new AttributeConfig();
        config.setKey(key);
        config.setFilteringType(AttributeType.NUMERIC);
        AttributeParserConfig parser = new AttributeParserConfig();
        parser.setDimension(dimension);
        parser.setDefaultUnitHint(defaultUnitHint);
        config.setParser(parser);
        attributesConfig.setConfigs(List.of(config));
        verticalConfig.setAttributesConfig(attributesConfig);
        return verticalConfig;
    }
}
