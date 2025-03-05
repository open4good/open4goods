package org.open4goods.services.prompt.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mockito.Mockito;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class GenAiServiceTestConfig {

    @Bean
    @Primary
    GenAiService genAiService() throws IOException, InterruptedException, ResourceNotFoundException, SerialisationException {
        // Create a Mockito mock that simulates common behavior
    	GenAiService mockService = Mockito.mock(GenAiService.class);
        // Configure default behavior (if applicable)
        Mockito.when(mockService.prompt(Mockito.any(), Mockito.any()))
               .thenAnswer(invocation -> {
                   // Return a dummy response – customize as needed
                   return promptResponse();
               });
        
        
        Mockito.when(mockService.jsonPrompt(Mockito.any(), Mockito.any()))
        .thenAnswer(invocation -> {
            // Return a dummy response – customize as needed
            return jsonPromptResponse();
        });
 
 
        
        return mockService;
    }

	private PromptResponse<Map<String, Object>> jsonPromptResponse() {
		PromptResponse<Map<String, Object>> ret = new PromptResponse<Map<String,Object>>();
		ret.setBody(new HashMap<String, Object>());
		ret.setDuration(1);
		ret.setPrompt(new PromptConfig());
		ret.setRaw("json raw");
		ret.setStart(System.currentTimeMillis());
		return ret;
	}

	private PromptResponse<String> promptResponse() {
		PromptResponse<String> ret = new PromptResponse<String>();
		ret.setBody("prompt rersponse body");
		ret.setDuration(1);
		ret.setPrompt(new PromptConfig());
		ret.setRaw("raw");
		ret.setStart(System.currentTimeMillis());
		return ret;
	}
}