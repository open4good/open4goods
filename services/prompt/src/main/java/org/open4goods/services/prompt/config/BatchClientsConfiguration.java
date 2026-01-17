package org.open4goods.services.prompt.config;

import org.open4goods.services.prompt.service.VertexGeminiBatchClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for batch inference clients.
 */
@Configuration
public class BatchClientsConfiguration {

    @Bean
    public VertexGeminiBatchClient vertexGeminiBatchClient(VertexBatchConfig config, ObjectMapper objectMapper) {
        return new VertexGeminiBatchClient(config, objectMapper);
    }
}
