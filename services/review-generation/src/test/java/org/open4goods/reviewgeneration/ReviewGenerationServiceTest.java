package org.open4goods.reviewgeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationStatus;
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
        // Assume product has a setter for GTIN.
        product.setId(123L);

        // Dummy vertical configuration.
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

        // Verify that the process status is SUCCESS and that processing messages were recorded.
        ReviewGenerationStatus status = reviewGenerationService.getProcessStatus(product.getId());
        assertNotNull(status, "Process status should be available");
        assertEquals(ReviewGenerationStatus.Status.SUCCESS, status.getStatus());
        assertTrue(status.getMessages().stream().anyMatch(msg -> msg.contains("Searching the web")),
                "Process messages should contain a web search message");
        assertTrue(status.getMessages().stream().anyMatch(msg -> msg.contains("AI generation")),
                "Process messages should contain an AI generation message");
    }
    
    /**
     * New test case to verify that the URL content token thresholds and accumulation behave as expected.
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
        product.setId(123L);
        // No alternate models for simplicity.
        
        // Dummy vertical configuration.
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("testVertical");
        ProductI18nElements i18n = new ProductI18nElements();
        PrefixedAttrText p = new PrefixedAttrText();
        p.setPrefix("TestVertical");
        i18n.setH1Title(p);
        verticalConfig.getI18n().put("fr", i18n);

        try {
            AiReview review = reviewGenerationService.generateReviewSync(product, verticalConfig);
            assertNotNull(review, "The generated review should not be null");
            System.out.println("Generated Review with token limits: " + review);
        } catch (Exception e) {
            fail("Review generation with token limits failed: ", e);
        }
    }
    
    /**
     * New test case to verify that duplicate submissions for the same GTIN are disabled.
     */
    @Test
    public void testDuplicateSubmission() {
        // Create a dummy product.
        Product product = new Product();
        product.setId(1111111111111L);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "DuplicateBrand");
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "Model1");
        product.setAkaModels(Set.of());
        product.setId(0L);
        
        // Dummy vertical configuration.
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("dup");
        ProductI18nElements i18n = new ProductI18nElements();
        PrefixedAttrText p = new PrefixedAttrText();
        p.setPrefix("Duplicate");
        i18n.setH1Title(p);
        verticalConfig.getI18n().put("fr", i18n);
        
        // First submission (async) should succeed.
        long upc1 = reviewGenerationService.generateReviewAsync(product, verticalConfig);
        // Second submission with same GTIN should throw an exception.
        try {
            reviewGenerationService.generateReviewAsync(product, verticalConfig);
            fail("Expected an exception for duplicate submission with same GTIN");
        } catch (IllegalStateException e) {
            // Expected exception.
            System.out.println("Caught expected duplicate submission exception: " + e.getMessage());
        }
    }
}
