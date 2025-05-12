package org.open4goods.googlesearch.mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import org.open4goods.services.googlesearch.config.GoogleSearchConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.exception.GoogleSearchException;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

/**
 * Test configuration for providing a primary GoogleSearchService bean for tests.
 * <p>
 * It systematically attempts to load a recorded mock response using Spring's classpath resource mechanism.
 * The resource is expected under "googlesearch/mocks/". If not found and if recording is enabled with a record folder configured,
 * it falls back to reading from that folder. Otherwise, it returns a dummy response.
 */
@TestConfiguration
public class GoogleSearchServiceMock {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSearchServiceMock.class);

    @Bean
    @Primary
    GoogleSearchService googleSearchService(GoogleSearchConfig properties, SerialisationService serialisationService)
            throws IOException, InterruptedException, GoogleSearchException {
        // Create a Mockito mock that simulates common behavior.
        GoogleSearchService mockService = Mockito.mock(GoogleSearchService.class);
        Mockito.when(mockService.search(Mockito.any())).thenAnswer(invocation -> {
            GoogleSearchRequest request = invocation.getArgument(0);
            // Sanitize the query to create a safe file name.
            String sanitizedQuery = GoogleSearchService.sanitizeUrlToFileName(request.getQuery());
            String fileName = sanitizedQuery + "-" + request.getNumResults() + ".json";
            // Define the path where mocks are expected in the classpath.
            String resourcePath = "googlesearch/mocks/" + fileName;

            // Attempt to load the recorded response from classpath using Spring's ClassPathResource.
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    logger.debug("Loaded mock response from classpath resource: {}", resourcePath);
                    return serialisationService.fromJson(json, GoogleSearchResponse.class);
                } catch (IOException e) {
                    logger.error("Failed to load recorded response from classpath: {}", e.getMessage());
                }
            } else {
                logger.debug("No classpath resource found at: {}", resourcePath);
            }

            // If not found in classpath, and if recording is enabled with a record folder, attempt to load from that folder.
            if (properties.isRecordEnabled() && properties.getRecordFolder() != null && !properties.getRecordFolder().isBlank()) {
                try {
                    Path filePath = Paths.get(properties.getRecordFolder()).resolve(fileName);
                    if (Files.exists(filePath)) {
                        String json = Files.readString(filePath);
                        logger.debug("Loaded mock response from folder: {}", filePath.toAbsolutePath());
                        return serialisationService.fromJson(json, GoogleSearchResponse.class);
                    } else {
                        logger.debug("No file found at folder path: {}", filePath.toAbsolutePath());
                    }
                } catch (IOException e) {
                    logger.error("Failed to load recorded response from folder: {}", e.getMessage());
                }
            }

            // Default dummy response if no recorded file is found.
            logger.warn("No recorded mock response found for request: {}. Returning dummy response.", request);
            List<GoogleSearchResult> res = new ArrayList<>();
            res.add(new GoogleSearchResult("link1", "http://link1"));
            res.add(new GoogleSearchResult("link2", "http://link2"));
            res.add(new GoogleSearchResult("link3", "http://link3"));
            return new GoogleSearchResponse(res);
        });
        return mockService;
    }
}
