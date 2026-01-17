package org.open4goods.services.prompt.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;

@Configuration
public class GeminiConfig {

    private static final Logger logger = LoggerFactory.getLogger(GeminiConfig.class);

    @Bean("geminiChatModel")
    @ConditionalOnProperty(prefix = "gen-ai-config", name = "google-api-json")
    public ChatModel geminiChatModel(PromptServiceConfig config) throws IOException {
        String jsonKey = config.getGoogleApiJson();
        
        if (!StringUtils.hasText(jsonKey)) {
             throw new IllegalArgumentException("google-api-json is missing in configuration");
        }

        // Parse Credentials
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(jsonKey.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        String projectId = config.getVertexProjectId(); 
        String location = config.getVertexLocation();

        // If projectId is not set in config, try to extract from credentials if possible
        // But for now, we rely on PromptServiceConfig having it or being set.
        // Or we can try to parse the JSON string manually to get "project_id"
        if (!StringUtils.hasText(projectId)) {
             // Basic manual extraction if needed, or fallback to ServiceOptions.getDefaultProjectId()
             // But let's look for "project_id" in jsonKey
             int pIdx = jsonKey.indexOf("\"project_id\"");
             if (pIdx > 0) {
                 int start = jsonKey.indexOf(":", pIdx) + 1;
                 while(jsonKey.charAt(start) == ' ' || jsonKey.charAt(start) == '"') start++;
                 int end = jsonKey.indexOf("\"", start);
                 projectId = jsonKey.substring(start, end);
             }
        }
        
        if (!StringUtils.hasText(projectId)) {
            throw new IllegalArgumentException("Vertex Project ID is missing and could not be extracted from google-api-json");
        }

        // Use Builder to configure VertexAI with credentials
        VertexAI vertexAi = new VertexAI.Builder()
                .setProjectId(projectId)
                .setLocation(location)
                .setCredentials(credentials)
                .build();

        return new VertexAiGeminiChatModel(vertexAi, VertexAiGeminiChatOptions.builder()
                .model("gemini-2.0-flash")
                .build());
    }
}
