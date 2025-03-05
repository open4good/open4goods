package org.open4goods.googlesearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mockito.Mockito;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class GoogleSearchServiceTestConfig {

    @Bean
    @Primary
    GoogleSearchService googleSearchService() throws IOException, InterruptedException {
        // Create a Mockito mock that simulates common behavior
        GoogleSearchService mockService = Mockito.mock(GoogleSearchService.class);
        // Configure default behavior (if applicable)
        Mockito.when(mockService.search(Mockito.any()))
               .thenAnswer(invocation -> {
                   // Return a dummy response – customize as needed
                   
                   List<GoogleSearchResult> res = new ArrayList<GoogleSearchResult>();
                   res.add(new GoogleSearchResult("link1", "http://link1"));
                   res.add(new GoogleSearchResult("link2", "http://link2"));
                   res.add(new GoogleSearchResult("link3", "http://link3"));
                   
                   GoogleSearchResponse ret = new GoogleSearchResponse(res);
                   return ret;
               });
        return mockService;
    }
}