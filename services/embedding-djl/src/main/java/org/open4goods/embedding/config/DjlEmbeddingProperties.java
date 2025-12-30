package org.open4goods.embedding.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties controlling DJL embedding model loading.
 */
@Validated
@ConfigurationProperties(prefix = "embedding")
public class DjlEmbeddingProperties
{

    /**
     * Toggle to enable or disable the DJL text embedding auto-configuration.
     */
    private boolean enabled = true;

    /**
     * Prefer loading models from the filesystem when a local path is provided.
     */
    private boolean preferLocalModels = true;

    /**
     * Local filesystem path to the text embedding model. When {@link #preferLocalModels}
     * is enabled and the path exists, it will be used instead of {@link #textModelUrl}.
     */
    private String textModelPath = "/opt/open4goods/models/text-embedding";

    /**
     * Local filesystem path to the multimodal text embedding model.
     */
    private String multimodalModelPath = "/opt/open4goods/models/multimodal-embedding";

    /**
     * Remote identifier for the text-only embedding model.
     */
    @NotBlank
    private String textModelUrl = "djl://ai.djl.huggingface.pytorch/intfloat/multilingual-e5-base";

    /**
     * Remote identifier for the multimodal embedding model (text branch used for fallback).
     */
    @NotBlank
    private String multimodalModelUrl = "djl://ai.djl.huggingface.pytorch/sentence-transformers/clip-ViT-B-32-multilingual-v1";

    /**
     * When true, startup fails if neither model can be loaded. If a local model is missing
     * and remote fallback is available, the application will still start unless both models fail.
     */
    private boolean failOnMissingModel = true;

    /**
     * Embedding dimension expected from loaded models; only used for informational purposes.
     */
    @Min(1)
    private int embeddingDimension = 512;

    /**
     * Pooling strategy applied by DJL translator.
     */
    @NotBlank
    private String poolingMode = "mean";

    /**
     * Whether to normalise the returned embedding vectors.
     */
    private boolean normalizeOutputs = true;

    /**
     * Engine name supplied to DJL criteria.
     */
    @NotBlank
    private String engine = "PyTorch";

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isPreferLocalModels()
    {
        return preferLocalModels;
    }

    public void setPreferLocalModels(boolean preferLocalModels)
    {
        this.preferLocalModels = preferLocalModels;
    }

    public String getTextModelPath()
    {
        return textModelPath;
    }

    public void setTextModelPath(String textModelPath)
    {
        this.textModelPath = textModelPath;
    }

    public String getMultimodalModelPath()
    {
        return multimodalModelPath;
    }

    public void setMultimodalModelPath(String multimodalModelPath)
    {
        this.multimodalModelPath = multimodalModelPath;
    }

    public String getTextModelUrl()
    {
        return textModelUrl;
    }

    public void setTextModelUrl(String textModelUrl)
    {
        this.textModelUrl = textModelUrl;
    }

    public String getMultimodalModelUrl()
    {
        return multimodalModelUrl;
    }

    public void setMultimodalModelUrl(String multimodalModelUrl)
    {
        this.multimodalModelUrl = multimodalModelUrl;
    }

    public boolean isFailOnMissingModel()
    {
        return failOnMissingModel;
    }

    public void setFailOnMissingModel(boolean failOnMissingModel)
    {
        this.failOnMissingModel = failOnMissingModel;
    }

    public int getEmbeddingDimension()
    {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension)
    {
        this.embeddingDimension = embeddingDimension;
    }

    public String getPoolingMode()
    {
        return poolingMode;
    }

    public void setPoolingMode(String poolingMode)
    {
        this.poolingMode = poolingMode;
    }

    public boolean isNormalizeOutputs()
    {
        return normalizeOutputs;
    }

    public void setNormalizeOutputs(boolean normalizeOutputs)
    {
        this.normalizeOutputs = normalizeOutputs;
    }

    public String getEngine()
    {
        return engine;
    }

    public void setEngine(String engine)
    {
        this.engine = engine;
    }
}
