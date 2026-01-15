package org.open4goods.services.reviewgeneration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.Localisable;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
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
                preprocessingService
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
        reviews.put("fr", holder);
        product.setReviews(reviews);

        boolean result = invokeShouldGenerateReview(product);

        assertThat(result).isFalse();
    }


    
    // Helper to invoke private method
    private boolean invokeShouldGenerateReview(Product product) {
        return ReflectionTestUtils.invokeMethod(reviewGenerationService, "shouldGenerateReview", product);
    }
}
