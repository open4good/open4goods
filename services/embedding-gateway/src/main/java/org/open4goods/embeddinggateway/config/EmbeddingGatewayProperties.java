package org.open4goods.embeddinggateway.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties controlling the embedding gateway behaviour.
 */
@ConfigurationProperties(prefix = "embedding.gateway")
@Validated
public class EmbeddingGatewayProperties
{
    @Min(1)
    private int maxConcurrentTextRequests = 16;

    @Min(1)
    private int maxConcurrentImageRequests = 8;

    @NotNull
    private Duration textRequestTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration imageRequestTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration imageDownloadTimeout = Duration.ofSeconds(10);

    @NotBlank
    private String imageModelUrl = "djl://ai.djl.huggingface.pytorch/sentence-transformers/clip-ViT-B-32-multilingual-v1";

    @Min(1)
    private int imageInputSize = 224;

    public int getMaxConcurrentTextRequests()
    {
        return maxConcurrentTextRequests;
    }

    public void setMaxConcurrentTextRequests(int maxConcurrentTextRequests)
    {
        this.maxConcurrentTextRequests = maxConcurrentTextRequests;
    }

    public int getMaxConcurrentImageRequests()
    {
        return maxConcurrentImageRequests;
    }

    public void setMaxConcurrentImageRequests(int maxConcurrentImageRequests)
    {
        this.maxConcurrentImageRequests = maxConcurrentImageRequests;
    }

    public Duration getTextRequestTimeout()
    {
        return textRequestTimeout;
    }

    public void setTextRequestTimeout(Duration textRequestTimeout)
    {
        this.textRequestTimeout = textRequestTimeout;
    }

    public Duration getImageRequestTimeout()
    {
        return imageRequestTimeout;
    }

    public void setImageRequestTimeout(Duration imageRequestTimeout)
    {
        this.imageRequestTimeout = imageRequestTimeout;
    }

    public Duration getImageDownloadTimeout()
    {
        return imageDownloadTimeout;
    }

    public void setImageDownloadTimeout(Duration imageDownloadTimeout)
    {
        this.imageDownloadTimeout = imageDownloadTimeout;
    }

    public String getImageModelUrl()
    {
        return imageModelUrl;
    }

    public void setImageModelUrl(String imageModelUrl)
    {
        this.imageModelUrl = imageModelUrl;
    }

    public int getImageInputSize()
    {
        return imageInputSize;
    }

    public void setImageInputSize(int imageInputSize)
    {
        this.imageInputSize = imageInputSize;
    }
}
