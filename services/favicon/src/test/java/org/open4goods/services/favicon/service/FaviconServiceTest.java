package org.open4goods.services.favicon.service;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for FaviconService.
 */
@SpringBootTest(classes = {FaviconServiceTest.TestConfig.class})
@ActiveProfiles("test")
// TODO : Could enhance thetest : Setup a test web server, serve html and favicons from src/test/resources, check for expected behavior
public class FaviconServiceTest {

    @Autowired
    private FaviconService faviconService;

    @Test
    public void testHasFavicon_InvalidUrl() {
        String url = "invalid-url";
        assertFalse(faviconService.hasFavicon(url));
    }
    
    @Test
    public void testNegativeCaching() {
        String url = "http://nonexistent.example.com/favicon.ico";
        // First call triggers remote fetch (and failure), caching the negative result.
        boolean existsFirst = faviconService.hasFavicon(url);
        // Second call should hit the negative cache and return quickly.
        boolean existsSecond = faviconService.hasFavicon(url);
        assertFalse(existsFirst, "Expected no favicon for nonexistent URL on first attempt.");
        assertFalse(existsSecond, "Expected no favicon for nonexistent URL on second attempt (cached).");
    }

    @Test
    public void testClearCache() {
        faviconService.clearCache();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"org.open4goods.services.favicon", "org.open4goods.commons.services"})
    public static class TestConfig {
        /**
         * Provide a RemoteFileCachingService bean for testing.
         */
        @Bean
        public RemoteFileCachingService remoteFileCachingService() {
            // Use a temporary folder for caching during tests.
            return new RemoteFileCachingService("target/favicon-cache");
        }
    }
}
