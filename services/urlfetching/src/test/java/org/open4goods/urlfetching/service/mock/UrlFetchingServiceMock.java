package org.open4goods.urlfetching.service.mock;

import java.util.concurrent.CompletableFuture;

import org.mockito.Mockito;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class UrlFetchingServiceMock {

    @Bean
    @Primary
    UrlFetchingService urlFetchingService() {
        // Create a Mockito mock of UrlFetchingService.
        UrlFetchingService mockService = Mockito.mock(UrlFetchingService.class);

        // Configure the mock to return different responses based on the URL input.
        Mockito.when(mockService.fetchUrl(Mockito.anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0, String.class);
            if (url.contains("example.com")) {
                // Simulate a successful fetch for example.com
                return CompletableFuture.completedFuture(
                        new FetchResponse(200, "<html>Example Domain</html>", "Example Domain"));
            } else if (url.contains("error.com")) {
                // Simulate an error scenario by returning a failed future
                CompletableFuture<FetchResponse> errorFuture = new CompletableFuture<>();
                errorFuture.completeExceptionally(new RuntimeException("Simulated error for error.com"));
                return errorFuture;
            } else if (url.contains("delayed.com")) {
                // Simulate a delayed response asynchronously
                return CompletableFuture.supplyAsync(() ->
                        new FetchResponse(200, "<html>Delayed Response</html>", "Delayed Response"));
            } else if (url == null || url.trim().isEmpty()) {
                // Optionally, simulate an invalid URL scenario
                CompletableFuture<FetchResponse> invalidFuture = new CompletableFuture<>();
                invalidFuture.completeExceptionally(new IllegalArgumentException("Invalid URL provided"));
                return invalidFuture;
            } else {
                // Default successful response for all other URLs
                return CompletableFuture.completedFuture(
                        new FetchResponse(200, "<html>Default Response</html>", "Default Response"));
            }
        });

        return mockService;
    }
}
