package org.open4goods.services.reviewgeneration.service;

import org.open4goods.model.attribute.ReferentielKey;

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
import org.open4goods.services.reviewgeneration.dto.AttributeExtractionResult;
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
    void generateReviewAsync_ShouldNotPersistRawTextModelAttributes() throws Exception {
        // The text-generation model must NOT write attributes back into the product.
        // Attributes are owned by the dedicated extraction stage, which validates them
        // against the vertical's canonical attribute configs. A non-canonical attribute
        // emitted by the text model (here "Display Size", absent from the empty test
        // vertical) must therefore never end up on the product.
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

        // Mock AI Response: the text model emits an attribute that the new contract ignores.
        AiReview aiReview = new AiReview();
        aiReview.setDescription("A valid review description longer than 20 characters.");
        aiReview.setAttributes(List.of(
            new AiReview.AiAttribute("Display Size", "55 inches", 1)
        ));

        PromptResponse<AiReview> promptResponse = new PromptResponse<>();
        promptResponse.setBody(aiReview);
        promptResponse.setMetadata(new HashMap<>());

        // Two-phase: mock attribute extraction (phase 1) and text generation (phase 2).
        PromptResponse<AttributeExtractionResult> attrResponse = new PromptResponse<>();
        attrResponse.setBody(new AttributeExtractionResult(aiReview.getAttributes()));
        when(genAiService.objectPrompt(anyString(), any(), eq(AttributeExtractionResult.class))).thenReturn(attrResponse);
        when(genAiService.objectPrompt(anyString(), any(), eq(AiReview.class))).thenReturn(promptResponse);

        // Execute
        reviewGenerationService.generateReviewAsync(product, verticalConfig, null, false);

        // Wait for asynchronous execution
        waitForCompletion(1001L);

        // Assert success
        ReviewGenerationStatus status = reviewGenerationService.getProcessStatus(1001L);
        assertThat(status.getStatus()).isEqualTo(ReviewGenerationStatus.Status.SUCCESS);

        // The non-canonical text-model attribute must NOT have been merged into the product.
        assertThat(product.getAttributes().getAll()).doesNotContainKey("Display Size");

        // The persisted review carries only validated product attributes (none here).
        AiReview persisted = status.getResult().getReview();
        assertThat(persisted).isNotNull();
        assertThat(persisted.getAttributes()).isEmpty();
    }

    @Test
    void testLocaleVariantContentPreservedBelowThreshold() throws Exception {
        // Create a real PreprocessingService for this test
        ReviewGenerationPreprocessingService realPreprocessing = new ReviewGenerationPreprocessingService(
                properties,
                googleSearchService,
                urlFetchingService,
                genAiService,
                new org.open4goods.services.serialisation.service.SerialisationService(),
                meterRegistry
        );

        // Setup product and config
        Product product = new Product(5901292525859L);
        product.setReviews(new Localisable<>());
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "TCL");
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "98P8K");

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("tv");

        // Two locale-variant URLs from same root host and canonical path
        String url1 = "https://www.tcl.com/eu/en/tvs/98p8k";
        String url2 = "https://www.tcl.com/hk/en/tvs/98p8k";

        when(googleSearchService.search(any())).thenReturn(new org.open4goods.services.googlesearch.dto.GoogleSearchResponse(List.of(
                new org.open4goods.services.googlesearch.dto.GoogleSearchResult("TCL 98P8K EU", url1),
                new org.open4goods.services.googlesearch.dto.GoogleSearchResult("TCL 98P8K HK", url2)
        )));

        // Both URLs return contents with similarity >= 0.8 (e.g. 0.96)
        String content1 = "TCL TV 98P8K review specs and details 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 EU";
        String content2 = "TCL TV 98P8K review specs and details 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 HK";

        when(urlFetchingService.fetchUrlAsync(eq(url1), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(
                        new org.open4goods.services.urlfetching.dto.FetchResponse(url1, 200, content1, content1, org.open4goods.services.urlfetching.config.FetchStrategy.HTTP)
                )
        );
        when(urlFetchingService.fetchUrlAsync(eq(url2), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(
                        new org.open4goods.services.urlfetching.dto.FetchResponse(url2, 200, content2, content2, org.open4goods.services.urlfetching.config.FetchStrategy.HTTP)
                )
        );

        // Map token estimations: 2100 tokens each
        when(genAiService.estimateTokens(content1)).thenReturn(2100);
        when(genAiService.estimateTokens(content2)).thenReturn(2100);

        properties.setMinMarkdownChars(5);
        properties.setSourceMinTokens(100);

        // When we run preprocessing, we expect both URLs to be kept to meet the threshold (complete: 4000, 2 URLs).
        // Let's execute preprocessing!
        Map<String, Object> variables = realPreprocessing.preparePromptVariables(product, verticalConfig, new ReviewGenerationStatus());

        assertThat(variables).containsKey("sources");
        @SuppressWarnings("unchecked")
        Map<String, String> sources = (Map<String, String>) variables.get("sources");
        
        // Assert both variants were preserved to satisfy the 4000 token / 2 URL complete threshold
        assertThat(sources).containsKey(url1);
        assertThat(sources).containsKey(url2);
        
        @SuppressWarnings("unchecked")
        Map<String, String> rejected = (Map<String, String>) variables.get("REJECTED_URLS");
        assertThat(rejected).doesNotContainKey(url1);
        assertThat(rejected).doesNotContainKey(url2);
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
