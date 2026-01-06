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
     * Remote identifier for the text-only embedding model.
     */
    @NotBlank
    private String textModelUrl = "djl://ai.djl.huggingface.pytorch/intfloat/multilingual-e5-small";



    /**
     * When true, startup fails if neither model can be loaded.
     */
    private boolean failOnMissingModel = true;

    /**
     * Embedding dimension expected from loaded models; only used for informational purposes.
     */
    @Min(1)
    private int embeddingDimension = 512;

    /**
     * Input size (in pixels) used for vision models.
     */
    @Min(1)
    private int imageInputSize = 224;

    /**
     * Pooling strategy applied by DJL translator.
     */
    @NotBlank
    private String poolingMode = "mean";

    /**
     * Prefix added to query text when generating embeddings.
     */
    @NotBlank
    private String queryPrefix = "query:";

    /**
     * Prefix added to passage text when generating embeddings.
     */
    @NotBlank
    private String passagePrefix = "passage:";

    /**
     * Engine name supplied to DJL criteria.
     */
    @NotBlank
    private String engine = "PyTorch";

    /**
     * Remote identifier for the image embedding (vision) model.
     */
    @NotBlank
    private String visionModelUrl = "djl://ai.djl.pytorch/resnet18_embedding/0.0.1";

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getTextModelUrl()
    {
        return textModelUrl;
    }

    public void setTextModelUrl(String textModelUrl)
    {
        this.textModelUrl = textModelUrl;
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

    public int getImageInputSize()
    {
        return imageInputSize;
    }

    public void setImageInputSize(int imageInputSize)
    {
        this.imageInputSize = imageInputSize;
    }

    public String getPoolingMode()
    {
        return poolingMode;
    }

    public void setPoolingMode(String poolingMode)
    {
        this.poolingMode = poolingMode;
    }

    public String getQueryPrefix()
    {
        return queryPrefix;
    }

    public void setQueryPrefix(String queryPrefix)
    {
        this.queryPrefix = queryPrefix;
    }

    public String getPassagePrefix()
    {
        return passagePrefix;
    }

    public void setPassagePrefix(String passagePrefix)
    {
        this.passagePrefix = passagePrefix;
    }

    public String getEngine()
    {
        return engine;
    }

    public void setEngine(String engine)
    {
        this.engine = engine;
    }

    public String getVisionModelUrl()
    {
        return visionModelUrl;
    }

    public void setVisionModelUrl(String visionModelUrl)
    {
        this.visionModelUrl = visionModelUrl;
    }
}
