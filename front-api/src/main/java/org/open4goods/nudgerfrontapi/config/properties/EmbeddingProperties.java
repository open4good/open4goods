package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties("front.embedding")
public class EmbeddingProperties {

    @NotBlank(message = "front.embedding.api-base-url must be provided")
    private String apiBaseUrl;

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
}
