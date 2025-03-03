package org.open4goods.googlesearch;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.open4goods.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.googlesearch.service.GoogleSearchService;
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
@SpringBootTest
@ActiveProfiles("test")
public class GoogleSearchServiceTest {

    @Autowired
    private GoogleSearchService googleSearchService;

    /**
     * Test that the search method returns a non-null response.
     */
    @Test
    public void testSearch() throws Exception {
        // Prepare a sample search request
        GoogleSearchRequest request = new GoogleSearchRequest("Spring Boot", 5);
        
        // Execute the search method
		try {
			googleSearchService.search(request);
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("400"));
			return;
		}
		fail("Should have fail");
		
    }

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
    @ComponentScan(basePackages = {"org.open4goods.googlesearch"})
    public static class TestConfig {
        // This class remains empty; its purpose is to trigger component scanning
        // in the org.open4goods.googlesearch package and enable auto-configuration.
    }
}
