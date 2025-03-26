package org.open4goods.services.favicon.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.open4goods.services.favicon.dto.FaviconResponse;
import org.open4goods.services.favicon.exception.FaviconException;
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
// TODO : Could enhance the test : Setup a test web server, serve html and favicons from src/test/resources, check for expected behavior
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
    
//    @Test
//    public void testFallbackFavicon() {
//        String url = "http://nonexistentfallback.example.com";
//        FaviconResponse response = null;
//        try {
//            response = faviconService.getFavicon(url);
//        } catch (FaviconException e) {
//            // In case of exception, the test should fail.
//        }
//        assertNotNull(response, "Expected fallback favicon response.");
//        assertTrue(response.faviconData().length > 0, "Fallback favicon data should not be empty.");
//    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"org.open4goods.services.favicon", "org.open4goods.commons.services"})
    public static class TestConfig {
        /**
         * Provide a RemoteFileCachingService bean for testing using a fake implementation.
         */
        @Bean
        public RemoteFileCachingService remoteFileCachingService() {
            return new FakeRemoteFileCachingService("target/favicon-cache");
        }
        
        /**
         * Fake implementation of RemoteFileCachingService for testing fallback behavior.
         */
        public static class FakeRemoteFileCachingService extends RemoteFileCachingService {
            public FakeRemoteFileCachingService(String cacheFolder) {
                super(cacheFolder);
            }
            
            @Override
            public File getResource(String resourceLocation) throws IOException {
                if (resourceLocation.contains("t3.gstatic.com/faviconV2")) {
                    // Create a temporary file with dummy favicon data.
                    File tempFile = File.createTempFile("fallback-favicon", ".png");
                    byte[] dummyData = new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10}; // PNG signature bytes.
                    java.nio.file.Files.write(tempFile.toPath(), dummyData);
                    return tempFile;
                }
                // Simulate failure for other resources.
                return new File("nonexistent");
            }
        }
    }
}
