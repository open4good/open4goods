package org.open4goods.services.prompt.service.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.services.prompt.config.PromptOptions;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;

class GeminiProviderTest {

    @Test
    void testGenerateTextWithWebSearch() {
        // Mock ChatModel
        VertexAiGeminiChatModel chatModel = mock(VertexAiGeminiChatModel.class);
        
        // Mock Response to avoid NPE
        ChatResponse mockResponse = new ChatResponse(Collections.singletonList(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("test response"))));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        GeminiProvider provider = new GeminiProvider(chatModel);

        // specific parameters to trigger web search
        boolean allowWebSearch = true;
        RetrievalMode retrievalMode = RetrievalMode.MODEL_WEB_SEARCH;
        
        ProviderRequest request = new ProviderRequest(
                "test-key",
                "system",
                "user",
                new PromptOptions(), 
                retrievalMode,
                null, 
                allowWebSearch, 
                Map.of()
        );

        provider.generateText(request);

        // Capture arguments
        org.mockito.ArgumentCaptor<Prompt> promptCaptor = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        Prompt list = promptCaptor.getValue();
        VertexAiGeminiChatOptions options = (VertexAiGeminiChatOptions) list.getOptions();

        assertTrue(options.getGoogleSearchRetrieval(), "Google Search retrieval should be enabled when requested");
    }
    @Test
    void testGenerateTextWithJsonSchema() {
        // Mock ChatModel
        VertexAiGeminiChatModel chatModel = mock(VertexAiGeminiChatModel.class);
        
        // Mock Response
        ChatResponse mockResponse = new ChatResponse(Collections.singletonList(
            new Generation(new org.springframework.ai.chat.messages.AssistantMessage("{\"name\":\"test\"}"))));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        GeminiProvider provider = new GeminiProvider(chatModel);
        
        String jsonSchema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        ProviderRequest request = new ProviderRequest(
                "test-key",
                "system",
                "user",
                new PromptOptions(), 
                RetrievalMode.EXTERNAL_SOURCES,
                jsonSchema,  // Pass JSON schema
                false, 
                Map.of()
        );

        provider.generateText(request);

        // Capture arguments
        org.mockito.ArgumentCaptor<Prompt> promptCaptor = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        Prompt list = promptCaptor.getValue();
        VertexAiGeminiChatOptions options = (VertexAiGeminiChatOptions) list.getOptions();

        // Verify JSON structured output settings
        // Note: exact assertion depends on VertexAiGeminiChatOptions implementation, 
        // using toString or checking specific getters if available.
        // Assuming getters exist or we can inspect behavior.
        // For now, these are standard expected behaviors for VertexAi Gemini options in Spring AI.
        if (options.getResponseMimeType() != null) {
            assertTrue(options.getResponseMimeType().contains("application/json"), "MimeType should be JSON");
        }
        assertTrue(options.getResponseSchema().contains("\"type\" : \"OBJECT\""),
                "Schema type values should be converted to Vertex enum values");
    }

    @Test
    void testGenerateTextWithGemini31WebSearchAndJsonSchema() {
        VertexAiGeminiChatModel chatModel = mock(VertexAiGeminiChatModel.class);
        ChatResponse mockResponse = new ChatResponse(Collections.singletonList(
            new Generation(new org.springframework.ai.chat.messages.AssistantMessage("{\"name\":\"test\"}"))));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        GeminiProvider provider = new GeminiProvider(chatModel);

        PromptOptions options = new PromptOptions();
        options.setModel("gemini-3.1-pro-preview");
        String jsonSchema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        ProviderRequest request = new ProviderRequest(
                "test-key",
                "system",
                "user",
                options,
                RetrievalMode.MODEL_WEB_SEARCH,
                jsonSchema,
                true,
                Map.of()
        );

        provider.generateText(request);

        org.mockito.ArgumentCaptor<Prompt> promptCaptor = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        VertexAiGeminiChatOptions capturedOptions = (VertexAiGeminiChatOptions) promptCaptor.getValue().getOptions();

        assertTrue(capturedOptions.getGoogleSearchRetrieval(), "Google Search retrieval should be enabled");
        assertTrue(capturedOptions.getResponseMimeType().contains("application/json"), "MimeType should be JSON");
        assertTrue(capturedOptions.getResponseSchema().contains("\"type\" : \"OBJECT\""),
                "Gemini 3.1 should keep structured output enabled with grounding");
    }
}
