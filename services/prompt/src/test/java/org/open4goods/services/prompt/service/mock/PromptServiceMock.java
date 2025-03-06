package org.open4goods.services.prompt.service.mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.mockito.Mockito;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Test configuration for providing a primary PromptService bean for tests.
 * <p>
 * It systematically attempts to load a recorded mock response using Spring's classpath resource mechanism.
 * The resource is expected under "prompt/mocks/". If not found and if recording is enabled with a record folder configured,
 * it falls back to reading from that folder. Otherwise, it returns a dummy response with the raw response prefixed by
 * "ORIGINAL PROMPT AS MOCKED RESPONSE:".
 * <p>
 * This configuration uses an embedded ObjectMapper for JSON serialization/deserialization and provides dummy beans for
 * the OpenAiApi dependencies.
 */
@TestConfiguration
public class PromptServiceMock {

    private static final Logger logger = LoggerFactory.getLogger(PromptServiceMock.class);

    @Bean
    @Primary
    public PromptService promptService(PromptServiceConfig properties,
                                       EvaluationService evaluationService,
                                       OpenAiApi openAiApi,
                                       OpenAiApi perplexityApi) throws ResourceNotFoundException, SerialisationException {
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
                   String fileName = promptKey + ".json";
                   String resourcePath = "prompt/mocks/" + fileName;

                   // Attempt to load the recorded response from the classpath.
                   ClassPathResource resource = new ClassPathResource(resourcePath);
                   if (resource.exists()) {
                       try (InputStream is = resource.getInputStream()) {
                           String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                           logger.debug("Loaded prompt mock from classpath: {}", resourcePath);
                           return objectMapper.readValue(json, new TypeReference<PromptResponse<CallResponseSpec>>() {});
                       } catch (IOException e) {
                           logger.error("Failed to load recorded response from classpath: {}", e.getMessage());
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
                               logger.debug("Loaded prompt mock from folder: {}", filePath.toAbsolutePath());
                               return objectMapper.readValue(json, new TypeReference<PromptResponse<CallResponseSpec>>() {});
                           } else {
                               logger.debug("No file found at folder path: {}", filePath.toAbsolutePath());
                           }
                       } catch (IOException e) {
                           logger.error("Failed to load recorded response from folder: {}", e.getMessage());
                       }
                   }

                   // Fallback: return a dummy response prefixed with the marker.
                   logger.warn("No recorded mock response found for promptKey: {}. Returning dummy response.", promptKey);
                   PromptResponse<CallResponseSpec> dummy = new PromptResponse<>();
                   dummy.setStart(System.currentTimeMillis());
                   dummy.setDuration(1);
                   String originalResponse = "dummy response";
                   dummy.setRaw("ORIGINAL PROMPT AS MOCKED RESPONSE: " + originalResponse);
//                   CallResponseSpec dummyBody = new CallResponseSpec() {
//                       @Override
//                       public String content() {
//                           return originalResponse;
//                       }
//                   };
//                   dummy.setBody(dummyBody);
                   dummy.setPrompt(new org.open4goods.services.prompt.config.PromptConfig());
                   return dummy;
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

    // Provide a dummy OpenAiApi bean.
    @Bean
    public OpenAiApi openAiApi() {
        return Mockito.mock(OpenAiApi.class);
    }

    // Provide a dummy perplexityApi bean.
    @Bean
    public OpenAiApi perplexityApi() {
        return Mockito.mock(OpenAiApi.class);
    }
}