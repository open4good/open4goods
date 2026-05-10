package org.open4goods.embedding.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
     * Text embedding backend provider.
     */
    @NotNull
    private Provider provider = Provider.DJL;

    /**
     * OpenAI-compatible embedding API settings, suitable for LocalAI.
     */
    @NotNull
    private OpenAiCompatible openai = new OpenAiCompatible();

    /**
     * Number of predictors to pool per model. Defaults to number of available processors.
     */
    @Min(1)
    private int predictorPoolSize = Runtime.getRuntime().availableProcessors();

    /**
     * Whether to load the machine learning models asynchronously at startup.
     */
    private boolean asyncLoading = true;

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

    public Provider getProvider()
    {
        return provider;
    }

    public void setProvider(Provider provider)
    {
        this.provider = provider;
    }

    public OpenAiCompatible getOpenai()
    {
        return openai;
    }

    public void setOpenai(OpenAiCompatible openai)
    {
        this.openai = openai;
    }

    /**
     * Returns the text embedding model identity used to invalidate cached vectors.
     *
     * @return provider and model endpoint identity
     */
    public String cacheModelIdentity()
    {
        if (provider == Provider.OPENAI_COMPATIBLE)
        {
            return provider + ":" + openai.getBaseUrl() + ":" + openai.getModel();
        }
        return provider + ":" + textModelUrl;
    }

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

    public int getPredictorPoolSize()
    {
        return predictorPoolSize;
    }

    public void setPredictorPoolSize(int predictorPoolSize)
    {
        this.predictorPoolSize = predictorPoolSize;
    }

    public boolean isAsyncLoading()
    {
        return asyncLoading;
    }

    public void setAsyncLoading(boolean asyncLoading)
    {
        this.asyncLoading = asyncLoading;
    }

    /**
     * Supported text embedding providers.
     */
    public enum Provider
    {
        DJL,
        OPENAI_COMPATIBLE
    }

    /**
     * Configuration for OpenAI-compatible embeddings endpoints.
     */
    public static class OpenAiCompatible
    {
        /**
         * Base API URL, without the /embeddings suffix.
         */
        @NotBlank
        private String baseUrl = "http://localhost:8080/v1";

        /**
         * API key sent as a bearer token. LocalAI accepts any non-empty value when auth is disabled.
         */
        @NotBlank
        private String apiKey = "localai";

        /**
         * Embedding model name exposed by the OpenAI-compatible server.
         */
        @NotBlank
        private String model = "text-embedding-model";

        /**
         * HTTP request timeout.
         */
        @NotNull
        private Duration timeout = Duration.ofSeconds(60);

        public String getBaseUrl()
        {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl)
        {
            this.baseUrl = baseUrl;
        }

        public String getApiKey()
        {
            return apiKey;
        }

        public void setApiKey(String apiKey)
        {
            this.apiKey = apiKey;
        }

        public String getModel()
        {
            return model;
        }

        public void setModel(String model)
        {
            this.model = model;
        }

        public Duration getTimeout()
        {
            return timeout;
        }

        public void setTimeout(Duration timeout)
        {
            this.timeout = timeout;
        }
    }
}
