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
}
