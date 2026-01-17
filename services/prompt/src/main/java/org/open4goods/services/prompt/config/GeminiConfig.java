package org.open4goods.services.prompt.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class GeminiConfig implements org.springframework.context.EnvironmentAware {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiConfig.class);

    private org.springframework.core.env.Environment env;

    @Override
    public void setEnvironment(org.springframework.core.env.Environment environment) {
        this.env = environment;
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public VertexAI vertexAi() throws IOException {
        String projectId = env.getProperty("spring.ai.google.genai.project-id");
        String location = env.getProperty("spring.ai.google.genai.location");
        String googleApiJson = env.getProperty("gen-ai-config.google-api-json");

        logger.info("****************************************************************");
        logger.info("Initializing VertexAI Bean with @Primary");
        logger.info("Active Profiles: {}", java.util.Arrays.toString(env.getActiveProfiles()));
        logger.info("Project ID: {}", projectId);
        logger.info("Location: {}", location);
        logger.info("Has Google API JSON: {}", StringUtils.hasText(googleApiJson));
        logger.info("****************************************************************");

        if (projectId == null || location == null) {
             throw new IllegalArgumentException("Project ID and Location are required for Vertex AI.");
        }

        if (googleApiJson != null && googleApiJson.trim().startsWith("{")) {
            logger.info("Configuring VertexAI with inline JSON credentials for project {} in {}", projectId, location);
            logger.info("JSON Prefix: {}", googleApiJson.trim().substring(0, Math.min(20, googleApiJson.trim().length())));
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(googleApiJson.getBytes(StandardCharsets.UTF_8)));
            
            return new VertexAI.Builder()
                .setProjectId(projectId)
                .setLocation(location)
                .setCredentials(credentials)
                .build();
        }
        
        logger.warn("No inline Google credentials found, using default authentication with project {}", projectId);
        return new VertexAI(projectId, location);
    }
}
