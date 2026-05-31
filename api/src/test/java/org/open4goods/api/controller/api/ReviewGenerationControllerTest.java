package org.open4goods.api.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationStepResult;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationVerticalResult;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.verticals.VerticalsConfigService;

@ExtendWith(MockitoExtension.class)
class ReviewGenerationControllerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VerticalsConfigService verticalsConfigService;

    @Mock
    private ReviewGenerationService reviewGenerationService;

    private ReviewGenerationController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewGenerationController(productRepository, verticalsConfigService, reviewGenerationService);
    }

    @Test
    void extractReviewAttributes_ShouldReturnPersistedSourceNumberedAttributes() throws Exception {
        Product product = product(123L, "tv");
        VerticalConfig verticalConfig = verticalConfig("tv");
        ReviewGenerationStepResult result = successfulResult(product, "attributes");

        when(productRepository.getById(123L)).thenReturn(product);
        when(verticalsConfigService.getConfigByIdOrDefault("tv")).thenReturn(verticalConfig);
        when(reviewGenerationService.extractReviewAttributes(product, verticalConfig)).thenReturn(result);

        ReviewGenerationStepResult body = controller.extractReviewAttributes(123L).getBody();

        assertThat(body).isNotNull();
        assertThat(body.acceptedUrls()).containsExactly("https://support.example.test/manual");
        assertThat(body.attributes())
                .singleElement()
                .satisfies(attribute -> {
                    assertThat(attribute.getName()).isEqualTo("screen_diagonal");
                    assertThat(attribute.getValue()).isEqualTo("55 in");
                    assertThat(attribute.getNumber()).isOne();
                });
        verify(reviewGenerationService).extractReviewAttributes(product, verticalConfig);
    }

    @Test
    void generateReviewWorkflowForVertical_ShouldExposeAcceptedUrlsAndSourceNumberedAttributes() throws Exception {
        Product product = product(123L, "tv");
        VerticalConfig verticalConfig = verticalConfig("tv");
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(verticalsConfigService.getConfigByIdOrDefault("tv")).thenReturn(verticalConfig);
        when(productRepository.exportVerticalWithValidDateOrderByImpactScore("tv", 1, false))
                .thenReturn(Stream.of(product));
        when(reviewGenerationService.generateReviewWorkflow(product, verticalConfig, Map.of()))
                .thenReturn(successfulResult(product, "workflow"));

        ReviewGenerationVerticalResult body = controller.generateReviewWorkflowForVertical("tv", 1, request)
                .getBody();

        assertThat(body).isNotNull();
        assertThat(body.processed()).isOne();
        assertThat(body.succeeded()).isOne();
        assertThat(body.products()).singleElement().satisfies(result -> {
            assertThat(result.acceptedUrls()).containsExactly("https://support.example.test/manual");
            assertThat(result.attributes())
                    .singleElement()
                    .satisfies(attribute -> {
                        assertThat(attribute.getName()).isEqualTo("screen_diagonal");
                        assertThat(attribute.getValue()).isEqualTo("55 in");
                        assertThat(attribute.getNumber()).isOne();
                    });
        });
        verify(reviewGenerationService).generateReviewWorkflow(product, verticalConfig, Map.of());
    }

    private Product product(long id, String vertical) {
        Product product = new Product();
        product.setId(id);
        product.setVertical(vertical);
        return product;
    }

    private VerticalConfig verticalConfig(String id) {
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId(id);
        return verticalConfig;
    }

    private ReviewGenerationStepResult successfulResult(Product product, String step) {
        return new ReviewGenerationStepResult(product.getId(), product.gtin(), product.getVertical(), step, true,
                "OK", 1, 12, "OK",
                List.of(new AiReview.AiAttribute("screen_diagonal", "55 in", 1)), null, null, List.of(),
                List.of("https://support.example.test/manual"), Map.of(), Map.of());
    }
}
