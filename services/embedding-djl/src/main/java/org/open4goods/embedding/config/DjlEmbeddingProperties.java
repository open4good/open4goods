package org.open4goods.embedding.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties controlling DJL image embedding model loading.
 */
@Validated
@ConfigurationProperties(prefix = "embedding")
public class DjlEmbeddingProperties
{
    /** Number of image predictors kept in the bounded pool. */
    @Min(1)
    private int predictorPoolSize = Runtime.getRuntime().availableProcessors();

    /** Whether to load the image model asynchronously at startup. */
    private boolean asyncLoading = true;

    /** Whether startup fails when the image model cannot be loaded. */
    private boolean failOnMissingModel = true;

    /** Input size in pixels used by the vision model. */
    @Min(1)
    private int imageInputSize = 224;

    /** Remote identifier for the image embedding model. */
    @NotBlank
    private String visionModelUrl = "djl://ai.djl.pytorch/resnet18_embedding/0.0.1";

    /**
     * Returns the configured predictor pool size.
     *
     * @return predictor count
     */
    public int getPredictorPoolSize()
    {
        return predictorPoolSize;
    }

    /**
     * Updates the predictor pool size.
     *
     * @param predictorPoolSize predictor count
     */
    public void setPredictorPoolSize(int predictorPoolSize)
    {
        this.predictorPoolSize = predictorPoolSize;
    }

    /**
     * Returns whether image-model loading is asynchronous.
     *
     * @return asynchronous loading flag
     */
    public boolean isAsyncLoading()
    {
        return asyncLoading;
    }

    /**
     * Updates the asynchronous loading flag.
     *
     * @param asyncLoading asynchronous loading flag
     */
    public void setAsyncLoading(boolean asyncLoading)
    {
        this.asyncLoading = asyncLoading;
    }

    /**
     * Returns whether a missing image model fails startup.
     *
     * @return fail-fast flag
     */
    public boolean isFailOnMissingModel()
    {
        return failOnMissingModel;
    }

    /**
     * Updates the fail-fast behavior for image-model loading.
     *
     * @param failOnMissingModel fail-fast flag
     */
    public void setFailOnMissingModel(boolean failOnMissingModel)
    {
        this.failOnMissingModel = failOnMissingModel;
    }

    /**
     * Returns the image input size.
     *
     * @return input size in pixels
     */
    public int getImageInputSize()
    {
        return imageInputSize;
    }

    /**
     * Updates the image input size.
     *
     * @param imageInputSize input size in pixels
     */
    public void setImageInputSize(int imageInputSize)
    {
        this.imageInputSize = imageInputSize;
    }

    /**
     * Returns the vision model location.
     *
     * @return model location
     */
    public String getVisionModelUrl()
    {
        return visionModelUrl;
    }

    /**
     * Updates the vision model location.
     *
     * @param visionModelUrl model location
     */
    public void setVisionModelUrl(String visionModelUrl)
    {
        this.visionModelUrl = visionModelUrl;
    }
}
