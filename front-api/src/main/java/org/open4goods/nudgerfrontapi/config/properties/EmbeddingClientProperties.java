package org.open4goods.nudgerfrontapi.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration for the embedding gateway HTTP client.
 */
@Validated
@ConfigurationProperties(prefix = "front.embedding-client")
public class EmbeddingClientProperties
{
    @NotBlank
    private String baseUrl = "http://localhost:8090";

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(2);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(5);

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout()
    {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout()
    {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout)
    {
        this.readTimeout = readTimeout;
    }
}
