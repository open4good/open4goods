package org.open4goods.services.reviewgeneration.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationPreprocessingService;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class ValidationTest {

    private ReviewGenerationService reviewGenerationService;
    private PromptService promptService;
    private ReviewGenerationPreprocessingService preprocessingService;
    private ProductRepository productRepository;

    @BeforeEach
    public void setup() {
        ReviewGenerationConfig config = new ReviewGenerationConfig();
        config.setThreadPoolSize(1);
        config.setMaxQueueSize(10);
        config.setBatchFolder("/tmp/batch"); 

        GoogleSearchService googleSearchService = mock(GoogleSearchService.class);
        UrlFetchingService urlFetchingService = mock(UrlFetchingService.class);
        promptService = mock(PromptService.class);
        BatchPromptService batchPromptService = mock(BatchPromptService.class);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        productRepository = mock(ProductRepository.class);
        preprocessingService = mock(ReviewGenerationPreprocessingService.class);

        reviewGenerationService = new ReviewGenerationService(config, googleSearchService, urlFetchingService, promptService, batchPromptService, meterRegistry, productRepository, preprocessingService);
    }

    @Test
    public void testGenerateReviewAsync_MissingVariable_ShouldFail() throws Exception {
        // Setup
        Product product = new Product();
        product.setId(123L);
        // Create a basic gtin to avoid NPE in isActiveForGtin or other checks if accessed
        // Product uses id for upc, and gtin() usually returns something or null
        // Let's assume default is null, but the service might access it. 
        // We will assert failure anyway.
        
        VerticalConfig verticalConfig = new VerticalConfig();

        PromptConfig promptConfig = new PromptConfig();
        promptConfig.setRetrievalMode(RetrievalMode.EXTERNAL_SOURCES);
        when(promptService.getPromptConfig(anyString())).thenReturn(promptConfig);

        // Mock preprocessing to return incomplete variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("PRODUCT_BRAND", "Brand");
        // Missing PRODUCT_MODEL, VERTICAL_NAME, etc.
        when(preprocessingService.preparePromptVariables(any(), any(), any())).thenReturn(variables);

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
}
