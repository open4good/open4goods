package org.open4goods.services.prompt.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;

@Configuration
public class GeminiConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiConfig.class);

    @org.springframework.beans.factory.annotation.Value("${spring.ai.google.genai.project-id}")
    private String projectId;

    @org.springframework.beans.factory.annotation.Value("${spring.ai.google.genai.location}")
    private String location;
    
    @org.springframework.beans.factory.annotation.Value("${gen-ai-config.google-api-json:}")
    private String googleApiJson;

    @Bean
    public Client googleGenAiClient() throws IOException {
        if (StringUtils.hasText(googleApiJson)) {
            logger.info("Configuring Google GenAI Client with inline JSON credentials for project {} in {}", projectId, location);
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(googleApiJson.getBytes(StandardCharsets.UTF_8)));
            
            return Client.builder()
                    .credentials(credentials)
                    .project(projectId)
                    .location(location)
                    .build();
        }
        
        logger.warn("No inline Google credentials found, using default authentication");
        return Client.builder()
                .project(projectId)
                .location(location)
                .build();
    }
}
