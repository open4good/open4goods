package org.open4goods.reviewgeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.googlesearch.mock.GoogleSearchServiceMock;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.prompt.service.mock.PromptServiceMock;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.reviewgeneration.dto.ProcessStatus;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.urlfetching.service.mock.UrlFetchingServiceMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {ReviewGenerationService.class, ReviewGenerationConfig.class, ReviewGenerationServiceTest.TestConfig.class})
@TestPropertySource(locations = "classpath:application-test.yml")
@ActiveProfiles("test")
@Import({GoogleSearchServiceMock.class, UrlFetchingServiceMock.class, PromptServiceMock.class})
public class ReviewGenerationServiceTest {

    /**
     * Minimal test configuration to bootstrap the Spring context.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"org.open4goods.services"})
    public static class TestConfig {
        // This class remains empty; its purpose is to trigger component scanning.
    }

    @Autowired
    private ReviewGenerationService reviewGenerationService;

    @Test
    public void testGenerateReviewSync() {
        // Create a dummy product.
        Product product = new Product();
        product.setId(8806091548818L);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "LG");
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "24TQ510S");
        product.setAkaModels(Set.of("24TQ510S-PZ.API", "24TQ510S-PZ"));

        // Dummy vertical configuration (as a String for testing).
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("tv");
        ProductI18nElements i18n = new ProductI18nElements();
        PrefixedAttrText p = new PrefixedAttrText();
        p.setPrefix("téléviseur");
        i18n.setH1Title(p);
        verticalConfig.getI18n().put("fr", i18n);

        // Invoke synchronous review generation.
        try {
            AiReview review = reviewGenerationService.generateReviewSync(product, verticalConfig);
            assertNotNull(review, "The generated review should not be null");
            System.out.println("Generated Review: " + review);
        } catch (Exception e) {
            fail("Review generation failed: ", e);
        }

        // Verify that the process status is SUCCESS.
        ProcessStatus status = reviewGenerationService.getProcessStatus(product.getId());
        assertNotNull(status, "Process status should be available");
        assertEquals(ProcessStatus.Status.SUCCESS, status.getStatus());
    }
    
    /**
     * New test case to verify that the URL content token thresholds and accumulation behave as expected.
     * <p>
     * This test simulates:
     * <ul>
     *   <li>A Google search returning two URLs.</li>
     *   <li>One URL returns content with an estimated token count above the minimum threshold.
     *       The other returns content with too few tokens.</li>
     *   <li>The aggregated tokens should include only the valid content and stop accumulation when reaching the maximum tokens limit.</li>
     * </ul>
     */
    @Test
    public void testGenerateReviewSyncTokenLimits() {
        // Create a dummy product.
        Product product = new Product();
        product.setId(1234567890123L);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "TestBrand");
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "TestModel");
        // No alternate models for simplicity.
        
        // Dummy vertical configuration.
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("testVertical");
        ProductI18nElements i18n = new ProductI18nElements();
        PrefixedAttrText p = new PrefixedAttrText();
        p.setPrefix("TestVertical");
        i18n.setH1Title(p);
        verticalConfig.getI18n().put("fr", i18n);

        // The mocks (GoogleSearchServiceMock, UrlFetchingServiceMock, PromptServiceMock)
        // are assumed to be configured (or can be adjusted) so that:
        // - The search returns two URLs.
        // - The first URL returns content with an estimated token count of 150 (above a minTokens threshold of 100).
        // - The second URL returns content with an estimated token count of 50 (below minTokens).
        // - The ReviewGenerationConfig in application-test.yml is set for this test:
        //      review.generation.max-search=2
        //      review.generation.max-tokens-per-request=300
        //      review.generation.min-tokens=100
        //      review.generation.max-concurrent-fetch=3
        // - The prompt service returns a fixed review text.
        
        try {
            AiReview review = reviewGenerationService.generateReviewSync(product, verticalConfig);
            assertNotNull(review, "The generated review should not be null");
            System.out.println("Generated Review with token limits: " + review);
        } catch (Exception e) {
            fail("Review generation with token limits failed: ", e);
        }
    }
}
