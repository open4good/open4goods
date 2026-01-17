package org.open4goods.googlesearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for {@link GoogleSearchService}.
 */
@SpringBootTest(classes= {GoogleSearchServiceTest.TestConfig.class})
@ActiveProfiles("test")
public class GoogleSearchServiceTest {

    @Autowired
    private GoogleSearchService googleSearchService;
    /**
     * Test that the search method returns an error for a bad request.
     * <p>
     * This test expects the search to fail with an error message containing "400".
     */
    // NOTE : disabled to avoid remote call
//    @Test
//    public void testSearch() throws IOException, InterruptedException {
//        // Prepare a sample search request with a known query.
//        GoogleSearchRequest request = new GoogleSearchRequest("Spring Boot", "lang_fr", "countryFR");
//
//        try {
//            googleSearchService.search(request);
//        } catch (Exception e) {
//            // We expect an error response due to test configuration (e.g., invalid API key)
//            assertTrue(e.getMessage().contains("400") || e.getMessage().contains("Error"));
//            return;
//        }
//        fail("Search should have failed with a 400 exception");
//    }

    /**
     * Test that the health check returns UP when properties are properly configured.
     */
    @Test
    public void testHealth() {
        // Verify that the health indicator returns a status of UP
        assertEquals(Status.UP, googleSearchService.health().getStatus(), "Health check should be UP");
    }

    /**
     * Minimal test configuration to bootstrap the Spring context.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"org.open4goods.services"})
    public static class TestConfig {
        // This class remains empty; its purpose is to trigger component scanning in the
        // org.open4goods.services.googlesearch package and enable auto-configuration.
    }
}
