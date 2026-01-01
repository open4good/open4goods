package org.open4goods.embedding.service;

import java.util.Optional;

import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Base class handling DJL model lifecycle and error reporting for embedding services.
 */
public abstract class AbstractDjlEmbeddingService implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDjlEmbeddingService.class);

    private final DjlEmbeddingProperties properties;
    private final AbstractTextModelFactory modelFactory;

    private ZooModel<String, float[]> textModel;
    private ZooModel<String, float[]> visionModel;
    private String resolvedTextModelLocation;
    private String resolvedMultimodalModelLocation;

    protected AbstractDjlEmbeddingService(DjlEmbeddingProperties properties, AbstractTextModelFactory modelFactory)
    {
        this.properties = properties;
        this.modelFactory = modelFactory;
    }

    @PostConstruct
    public void initialize()
    {
        resolvedTextModelLocation = properties.getTextModelUrl();
        resolvedMultimodalModelLocation = properties.getMultimodalModelUrl();

        textModel = tryLoad(resolvedTextModelLocation, "text");
        visionModel = tryLoad(resolvedMultimodalModelLocation, "multimodal");

        if (textModel == null && visionModel == null && properties.isFailOnMissingModel())
        {
            throw new IllegalStateException("Failed to load any DJL text embedding model (text or multimodal)");
        }
        LOGGER.info("DJL embedding initialised. textModelLoaded={}, multimodalLoaded={}, dimension={}.",
                textModel != null, visionModel != null, properties.getEmbeddingDimension());
    }

    public float[] embed(String text)
    {
        if (!StringUtils.hasText(text))
        {
            throw new IllegalArgumentException("Text to embed must not be null or blank");
        }

        float[] vector = embedWithModel(textModel, text, "text");
        if (vector != null)
        {
            return vector;
        }
        vector = embedWithModel(visionModel, text, "multimodal");
        if (vector != null)
        {
            return vector;
        }
        throw new IllegalStateException("No embedding model available to process the requested text");
    }

    public Optional<String> getTextModelLocation()
    {
        return Optional.ofNullable(resolvedTextModelLocation);
    }

    public Optional<String> getMultimodalModelLocation()
    {
        return Optional.ofNullable(resolvedMultimodalModelLocation);
    }

    public boolean isTextModelLoaded()
    {
        return textModel != null;
    }

    public boolean isMultimodalTextModelLoaded()
    {
        return visionModel != null;
    }

    @PreDestroy
    @Override
    public void close()
    {
        closeQuietly(textModel);
        closeQuietly(visionModel);
    }

    private ZooModel<String, float[]> tryLoad(String modelLocation, String label)
    {
        if (!StringUtils.hasText(modelLocation))
        {
            LOGGER.warn("{} model location is empty, skipping load", label);
            return null;
        }
        try
        {
            return modelFactory.loadModel(modelLocation, properties.getPoolingMode(), properties.getEngine());
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to initialize {} embedding model from {}", label, modelLocation, e);
            return null;
        }
    }

    private float[] embedWithModel(ZooModel<String, float[]> model, String text, String label)
    {
        if (model == null)
        {
            return null;
        }
        try (Predictor<String, float[]> predictor = model.newPredictor())
        {
            return predictor.predict(text);
        }
        catch (Exception e)
        {
            LOGGER.error("Error embedding text with {} model", label, e);
            return null;
        }
    }

    private void closeQuietly(ZooModel<String, float[]> model)
    {
        if (model != null)
        {
            model.close();
        }
    }
}
