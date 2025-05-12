package org.open4goods.urlfetching.service.mock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test configuration providing a Mockito-based mock for {@link UrlFetchingService} that
 * first checks for recorded files in the classpath (typically under {@code src/test/resources/urlfetching/mocks})
 * and returns them if present. If not found in the classpath, it falls back to scanning the file system.
 * If both methods fail, it returns dummy responses.
 * <p>
 * Recorded responses are stored in a designated folder (configured via properties) and
 * will not be played back outside of this test configuration.
 * </p>
 */
@TestConfiguration
@Profile("test")
public class UrlFetchingServiceMock {

    private static final Logger logger = LoggerFactory.getLogger(UrlFetchingServiceMock.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    // Folder where recorded responses are stored.
    private static final String DEFAULT_RECORD_FOLDER = "src/test/resources/urlfetching/mocks";

    /**
     * Sanitizes a URL into a safe file name by removing the protocol and replacing non-alphanumeric characters.
     *
     * @param url the URL to sanitize
     * @return a sanitized file name ending with .json
     */
    private static String sanitizeUrlToFileName(String url) {
        String sanitized = url.replaceFirst("^(https?://)", "").replaceAll("[^a-zA-Z0-9]", "_");
        return sanitized + ".txt";
    }

    @Bean
    @Primary
    public UrlFetchingService urlFetchingService() {
        UrlFetchingService mockService = org.mockito.Mockito.mock(UrlFetchingService.class);

        org.mockito.Mockito.when(mockService.fetchUrlAsync(org.mockito.ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> {
                String url = invocation.getArgument(0, String.class);
                String fileName = sanitizeUrlToFileName(url);

                // First, attempt to load the recorded response from the classpath.
                ClassPathResource cpResource = new ClassPathResource("urlfetching/mocks/" + fileName);
                if (cpResource.exists()) {
                    try (InputStream is = cpResource.getInputStream()) {
                        byte[] bytes = is.readAllBytes();
                        FetchResponse response = mapper.readValue(bytes, FetchResponse.class);
                        logger.info("Loaded recorded response for URL {} from classpath resource", url);
                        return CompletableFuture.completedFuture(response);
                    } catch (IOException e) {
                        logger.warn("Error reading classpath resource for URL {}: {}", url, e.getMessage());
                    }
                }

                // Fallback: Attempt to load the recorded response from the file system.
                File recordFile = new File(DEFAULT_RECORD_FOLDER, fileName);
                if (recordFile.exists()) {
                    try {
                        byte[] bytes = Files.readAllBytes(Paths.get(recordFile.getAbsolutePath()));
                        FetchResponse response = mapper.readValue(bytes, FetchResponse.class);
                        logger.info("Loaded recorded response for URL {} from file system", url);
                        return CompletableFuture.completedFuture(response);
                    } catch (IOException e) {
                        logger.warn("Error reading file system resource for URL {}: {}", url, e.getMessage());
                    }
                }

                // Fallback: Return predefined dummy responses based on URL content.
                if (url.contains("example.com")) {
                    return CompletableFuture.completedFuture(
                            new FetchResponse(url, 200, "<html>Example Domain</html>", "Example Domain", FetchStrategy.HTTP));
                } else if (url.contains("error.com")) {
                    CompletableFuture<FetchResponse> errorFuture = new CompletableFuture<>();
                    errorFuture.completeExceptionally(new RuntimeException("Simulated error for error.com"));
                    return errorFuture;
                } else if (url.contains("delayed.com")) {
                    return CompletableFuture.supplyAsync(() ->
                            new FetchResponse(url, 200, "<html>Delayed Response</html>", "Delayed Response", FetchStrategy.SELENIUM));
                } else if (url == null || url.trim().isEmpty()) {
                    CompletableFuture<FetchResponse> invalidFuture = new CompletableFuture<>();
                    invalidFuture.completeExceptionally(new IllegalArgumentException("Invalid URL provided"));
                    return invalidFuture;
                } else {
                    return CompletableFuture.completedFuture(
                            new FetchResponse(url, 200, "<html>Default Response</html>", "Default Response", FetchStrategy.HTTP));
                }
            });

        return mockService;
    }
}
