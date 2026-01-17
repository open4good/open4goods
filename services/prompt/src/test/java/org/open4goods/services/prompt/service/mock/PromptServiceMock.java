package org.open4goods.services.prompt.service.mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.*;
import java.util.Map;

import org.mockito.Mockito;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test configuration for providing a primary PromptService bean for tests.
 * <p>
 * It systematically attempts to load a recorded mock response using Spring's classpath resource mechanism.
 * The resource is expected under "prompt/mocks/". If not found and if recording is enabled with a record folder configured,
 * it falls back to reading from that folder. Otherwise, it returns a dummy response with the raw response prefixed by
 * "ORIGINAL PROMPT AS MOCKED RESPONSE:".
 * <p>
 * This configuration uses an embedded ObjectMapper for JSON serialization/deserialization.
 */
@TestConfiguration
public class PromptServiceMock {

    private static final Logger logger = LoggerFactory.getLogger(PromptServiceMock.class);

    @Bean
    @Primary
    public PromptService promptService(PromptServiceConfig properties,
                                       EvaluationService evaluationService) throws ResourceNotFoundException, SerialisationException {
        // Create an embedded ObjectMapper instance.
        ObjectMapper objectMapper = new ObjectMapper();

        // Create a Mockito mock for the PromptService.
        PromptService mockService = Mockito.mock(PromptService.class);

        // Configure the prompt() method.
        Mockito.when(mockService.prompt(Mockito.anyString(), Mockito.anyMap()))
               .thenAnswer(invocation -> {
                   String promptKey = invocation.getArgument(0);
                   @SuppressWarnings("unchecked")
                   Map<String, Object> variables = invocation.getArgument(1);
                   // Use the new naming convention for raw response files
                   String fileName = promptKey + "-response.txt";
                   String resourcePath = "prompt/mocks/" + fileName;

                   String rawContent = null;
                   // Attempt to load the recorded raw response from the classpath.
                   ClassPathResource resource = new ClassPathResource(resourcePath);
                   if (resource.exists()) {
                       try (InputStream is = resource.getInputStream()) {
                           rawContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                           logger.debug("Loaded prompt mock raw response from classpath: {}", resourcePath);
                       } catch (IOException e) {
                           logger.error("Failed to load recorded response from classpath: {}", e.getMessage());
                       }
                   } else {
                       logger.debug("No classpath resource found at: {}", resourcePath);
                   }

                   // If not found in classpath, try the folder if recording is enabled.
                   if (rawContent == null && properties.isRecordEnabled() && properties.getRecordFolder() != null && !properties.getRecordFolder().isBlank()) {
                       try {
                           Path filePath = Paths.get(properties.getRecordFolder()).resolve(fileName);
                           if (Files.exists(filePath)) {
                               rawContent = Files.readString(filePath, StandardCharsets.UTF_8);
                               logger.debug("Loaded prompt mock raw response from folder: {}", filePath.toAbsolutePath());
                           } else {
                               logger.debug("No file found at folder path: {}", filePath.toAbsolutePath());
                           }
                       } catch (IOException e) {
                           logger.error("Failed to load recorded response from folder: {}", e.getMessage());
                       }
                   }

                   // If still not found, use fallback dummy response.
                   if (rawContent == null) {
                       logger.warn("No recorded mock response found for promptKey: {}. Returning dummy response.", promptKey);
                       rawContent = "dummy response";
                   }

                   // Wrap the raw content in a dummy CallResponseSpec instance.
                   PromptResponse<String> response = new PromptResponse<>();
                   response.setStart(System.currentTimeMillis());
                   response.setDuration(1);
                   response.setRaw(rawContent);
                   response.setPrompt(new org.open4goods.services.prompt.config.PromptConfig());
                   response.setBody(rawContent);
                   return response;
               });

        // Configure the jsonPrompt() method.
        Mockito.when(mockService.jsonPrompt(Mockito.anyString(), Mockito.anyMap()))
               .thenAnswer(invocation -> {
                   String promptKey = invocation.getArgument(0);
                   @SuppressWarnings("unchecked")
                   Map<String, Object> variables = invocation.getArgument(1);
                   String fileName = promptKey + ".json";
                   String resourcePath = "prompt/mocks/" + fileName;

                   // Attempt to load the recorded JSON response from the classpath.
                   ClassPathResource resource = new ClassPathResource(resourcePath);
                   if (resource.exists()) {
                       try (InputStream is = resource.getInputStream()) {
                           String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                           logger.debug("Loaded prompt JSON mock from classpath: {}", resourcePath);
                           return objectMapper.readValue(json, new TypeReference<PromptResponse<Map<String, Object>>>() {});
                       } catch (IOException e) {
                           logger.error("Failed to load recorded JSON response from classpath: {}", e.getMessage());
                       }
                   } else {
                       logger.debug("No classpath resource found at: {}", resourcePath);
                   }

                   // If not found in classpath, try the folder if recording is enabled.
                   if (properties.isRecordEnabled() && properties.getRecordFolder() != null && !properties.getRecordFolder().isBlank()) {
                       try {
                           Path filePath = Paths.get(properties.getRecordFolder()).resolve(fileName);
                           if (Files.exists(filePath)) {
                               String json = Files.readString(filePath, StandardCharsets.UTF_8);
                               logger.debug("Loaded prompt JSON mock from folder: {}", filePath.toAbsolutePath());
                               return objectMapper.readValue(json, new TypeReference<PromptResponse<Map<String, Object>>>() {});
                           } else {
                               logger.debug("No file found at folder path: {}", filePath.toAbsolutePath());
                           }
                       } catch (IOException e) {
                           logger.error("Failed to load recorded JSON response from folder: {}", e.getMessage());
                       }
                   }

                   // Fallback: return a dummy JSON response prefixed with the marker.
                   logger.warn("No recorded JSON mock response found for promptKey: {}. Returning dummy response.", promptKey);
                   PromptResponse<Map<String, Object>> dummy = new PromptResponse<>();
                   dummy.setStart(System.currentTimeMillis());
                   dummy.setDuration(1);
                   String originalResponse = "dummy JSON response";
                   dummy.setRaw("ORIGINAL PROMPT AS MOCKED RESPONSE: " + originalResponse);
                   dummy.setBody(Map.of());
                   dummy.setPrompt(new org.open4goods.services.prompt.config.PromptConfig());
                   return dummy;
               });

        return mockService;
    }

}
