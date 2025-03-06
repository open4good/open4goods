package org.open4goods.urlfetching.service.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test configuration providing a Mockito-based mock for {@link UrlFetchingService} that
 * first checks for recorded files matching the URL and returns them if present.
 * <p>
 * Recorded responses are stored in a designated folder (configured via properties) and
 * will not be played back outside of this test configuration.
 * </p>
 */
@TestConfiguration
@Profile("test")
public class UrlFetchingServiceMock {

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
        return sanitized + ".json";
    }

    @Bean
    @Primary
    public UrlFetchingService urlFetchingService() {
        UrlFetchingService mockService = org.mockito.Mockito.mock(UrlFetchingService.class);

        org.mockito.Mockito.when(mockService.fetchUrl(org.mockito.ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> {
                String url = invocation.getArgument(0, String.class);

                // Check if a recorded file exists for this URL
                String fileName = sanitizeUrlToFileName(url);
                File recordFile = new File(DEFAULT_RECORD_FOLDER, fileName);
                if (recordFile.exists()) {
                    try {
                        byte[] bytes = Files.readAllBytes(Paths.get(recordFile.getAbsolutePath()));
                        FetchResponse response = mapper.readValue(bytes, FetchResponse.class);
                        return CompletableFuture.completedFuture(response);
                    } catch (IOException e) {
                        // In case of error reading the file, fall back to default response logic.
                    }
                }

                // Fallback: Return predefined responses based on URL content.
                if (url.contains("example.com")) {
                    return CompletableFuture.completedFuture(
                            new FetchResponse(200, "<html>Example Domain</html>", "Example Domain"));
                } else if (url.contains("error.com")) {
                    CompletableFuture<FetchResponse> errorFuture = new CompletableFuture<>();
                    errorFuture.completeExceptionally(new RuntimeException("Simulated error for error.com"));
                    return errorFuture;
                } else if (url.contains("delayed.com")) {
                    return CompletableFuture.supplyAsync(() ->
                            new FetchResponse(200, "<html>Delayed Response</html>", "Delayed Response"));
                } else if (url == null || url.trim().isEmpty()) {
                    CompletableFuture<FetchResponse> invalidFuture = new CompletableFuture<>();
                    invalidFuture.completeExceptionally(new IllegalArgumentException("Invalid URL provided"));
                    return invalidFuture;
                } else {
                    return CompletableFuture.completedFuture(
                            new FetchResponse(200, "<html>Default Response</html>", "Default Response"));
                }
            });

        return mockService;
    }
}
