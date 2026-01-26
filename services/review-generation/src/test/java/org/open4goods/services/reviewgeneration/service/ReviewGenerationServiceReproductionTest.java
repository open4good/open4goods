package org.open4goods.services.reviewgeneration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.Localisable;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.open4goods.verticals.VerticalsConfigService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class ReviewGenerationServiceReproductionTest {

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
        properties.setThreadPoolSize(1);
        properties.setMaxQueueSize(10);
        properties.setRegenerationDelayDays(30);
        properties.setRetryDelayDays(7);
        properties.setBatchFolder(System.getProperty("java.io.tmpdir") + "/batch-test-repro");
        properties.setResolveUrl(false);

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
                verticalsConfigService,
                Collections.emptyList()
        );
    }

    @Test
    void generateReviewAsync_ShouldSetCorrectDatasourceName_WhenProviderIsGemini() throws Exception {
        // Setup
        Product product = new Product();
        product.setId(1001L);
        product.setReviews(new Localisable<>()); // No reviews
        
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("test-vertical");

        // Mock Prompt Config
        PromptConfig promptConfig = new PromptConfig();
        promptConfig.setRetrievalMode(RetrievalMode.EXTERNAL_SOURCES);
        when(genAiService.getPromptConfig(anyString())).thenReturn(promptConfig);

        // Mock Preprocessing
        Map<String, Object> promptVariables = new HashMap<>();
        promptVariables.put("PRODUCT_BRAND", "TestBrand");
        promptVariables.put("PRODUCT_MODEL", "TestModel");
        promptVariables.put("VERTICAL_NAME", "TestVertical");
        promptVariables.put("OFFER_NAMES", "TestOffer");
        promptVariables.put("IMPACTSCORE_POSITION", "TestPosition");
        promptVariables.put("COMMON_ATTRIBUTES", "TestAttributes");
        
        when(preprocessingService.preparePromptVariables(any(), any(), any(), any())).thenReturn(promptVariables);

        // Mock AI Response
        AiReview aiReview = new AiReview();
        aiReview.setDescription("A valid review description longer than 20 characters.");
        aiReview.setAttributes(List.of(
            new AiReview.AiAttribute("Display Size", "55 inches", 1)
        ));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "gemini");
        metadata.put("model", "gemini-1.5-pro");

        PromptResponse<AiReview> promptResponse = new PromptResponse<>();
        promptResponse.setBody(aiReview);
        promptResponse.setMetadata(metadata);
        
        when(genAiService.objectPrompt(anyString(), any(), eq(AiReview.class))).thenReturn(promptResponse);

        // Execute
        reviewGenerationService.generateReviewAsync(product, verticalConfig, null, false);

        // Wait for asynchronous execution
        waitForCompletion(1001L);

        // Assert
        ReviewGenerationStatus status = reviewGenerationService.getProcessStatus(1001L);
        assertThat(status.getStatus()).isEqualTo(ReviewGenerationStatus.Status.SUCCESS);

        // Verify Product Attributes
        // Since we are mocking productRepository, existing attributes might be empty unless we put them or the service modified the product object directly (which it does)
        assertThat(product.getAttributes().getAll()).containsKey("Display Size");
        ProductAttribute attr = product.getAttributes().getAll().get("Display Size");
        assertThat(attr).isNotNull();
        assertThat(attr.getSource()).isNotEmpty();
        
        SourcedAttribute sourcedAttr = attr.getSource().iterator().next();
        assertThat(sourcedAttr.getDataSourcename()).isEqualTo("gemini");
    }

    private void waitForCompletion(long upc) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000) {
            ReviewGenerationStatus status = reviewGenerationService.getProcessStatus(upc);
            if (status != null && (status.getStatus() == ReviewGenerationStatus.Status.SUCCESS || status.getStatus() == ReviewGenerationStatus.Status.FAILED)) {
                return;
            }
            Thread.sleep(100);
        }
    }
}
