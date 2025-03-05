package org.open4goods.urlfetching.service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.mockito.Mockito;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class UrlFetchingServiceTestConfig {

	@Bean
    @Primary
    public UrlFetchingService urlFetchingService() throws IOException, InterruptedException {
        // Create a Mockito mock that simulates common behavior
    	UrlFetchingService mockService = Mockito.mock(UrlFetchingService.class);
        // Configure default behavior (if applicable)
        Mockito.when(mockService.fetchUrl(Mockito.any()))
               .thenAnswer(invocation -> {
                
				return getResponse();
               });
        return mockService;
    }

    private CompletableFuture<FetchResponse> getResponse() {
        FetchResponse response = new FetchResponse(200, "<html>coucou</html>", "coucou");
        return CompletableFuture.completedFuture(response);
    }
}