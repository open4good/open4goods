package org.open4goods.reviewgeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.googlesearch.mock.GoogleSearchServiceMock;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.prompt.service.mock.GenAiServiceMock;
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
@Import({GoogleSearchServiceMock.class, UrlFetchingServiceMock.class, GenAiServiceMock.class})

public class ReviewGenerationServiceTest {

    /**
     * Minimal test configuration to bootstrap the Spring context.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"org.open4goods.services.reviewgeneration"})
    public static class TestConfig {
        // This class remains empty; its purpose is to trigger component scanning.
   
    }

    @Autowired
    private ReviewGenerationService reviewGenerationService;

    @Test
    public void testGenerateReviewSync() {
        // Create a dummy product.
        Product product = new Product();
        product.setId(1234567890123L);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "SAMSUNG");
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "TestModel");
        product.setAkaModels(Set.of("TestModelAlt1", "TestModelAlt2"));

        // Dummy vertical configuration (as a String for testing).
        VerticalConfig verticalConfig = new VerticalConfig();

        // Invoke synchronous review generation.
        try {
            String review = reviewGenerationService.generateReviewSync(product, verticalConfig);
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
}
