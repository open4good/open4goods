package org.open4goods.embedding.service;

import java.util.Optional;

import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;
import jakarta.annotation.PreDestroy;

/**
 * Base class handling DJL model lifecycle and error reporting for embedding services.
 * <p>
 * Models are loaded eagerly in the constructor. {@link Predictor} instances are cached per-thread
 * via {@link ThreadLocal} to avoid the overhead of creating a new predictor on every call while
 * remaining thread-safe (DJL {@code Predictor} is not thread-safe).
 * </p>
 */
public abstract class AbstractDjlEmbeddingService implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDjlEmbeddingService.class);

    private final DjlEmbeddingProperties properties;
    private final AbstractTextModelFactory modelFactory;

    private final ZooModel<String, float[]> textModel;
    private final ZooModel<String, float[]> visionModel;
    private final String resolvedTextModelLocation;
    private final String resolvedVisionModelLocation;

    /**
     * Thread-local predictors: one per thread, lazily created from the loaded models.
     * This avoids the cost of {@code model.newPredictor()} on every {@link #embed} call
     * while keeping thread safety (DJL {@code Predictor} is not thread-safe).
     */
    private final ThreadLocal<Predictor<String, float[]>> textPredictor;
    private final ThreadLocal<Predictor<String, float[]>> visionPredictor;

    protected AbstractDjlEmbeddingService(DjlEmbeddingProperties properties, AbstractTextModelFactory modelFactory)
    {
        this.properties = properties;
        this.modelFactory = modelFactory;

        // Model loading (eagerly in constructor)
        resolvedTextModelLocation = properties.getTextModelUrl();
        resolvedVisionModelLocation = properties.getVisionModelUrl();

        textModel = tryLoad(resolvedTextModelLocation, "text");
        visionModel = tryLoad(resolvedVisionModelLocation, "multimodal");

        if (textModel == null && visionModel == null && properties.isFailOnMissingModel())
        {
            throw new IllegalStateException("Failed to load any DJL text embedding model (text or multimodal)");
        }

        // ThreadLocal predictors: lazily created per thread from the loaded models
        textPredictor = ThreadLocal.withInitial(() ->
                textModel != null ? textModel.newPredictor() : null);
        visionPredictor = ThreadLocal.withInitial(() ->
                visionModel != null ? visionModel.newPredictor() : null);

        LOGGER.info("DJL embedding initialised. textModelLoaded={}, multimodalLoaded={}, dimension={}.",
                textModel != null, visionModel != null, properties.getEmbeddingDimension());
    }

    /**
     * Embeds the given text using the first available model (text, then multimodal fallback).
     *
     * @param text the text to embed, must not be blank
     * @return the embedding vector
     * @throws IllegalArgumentException if the text is blank
     * @throws IllegalStateException    if no model is available
     */
    public float[] embed(String text)
    {
        if (!StringUtils.hasText(text))
        {
            throw new IllegalArgumentException("Text to embed must not be null or blank");
        }

        float[] vector = embedWithPredictor(textPredictor, text, "text");
        if (vector != null)
        {
            return vector;
        }
        vector = embedWithPredictor(visionPredictor, text, "multimodal");
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
        return Optional.ofNullable(resolvedVisionModelLocation);
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
        textPredictor.remove();
        visionPredictor.remove();
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

    /**
     * Runs prediction using the thread-local cached {@link Predictor}.
     *
     * @param predictorHolder thread-local predictor supplier
     * @param text            text to embed
     * @param label           label for logging
     * @return the embedding vector, or {@code null} if the predictor is unavailable
     */
    private float[] embedWithPredictor(ThreadLocal<Predictor<String, float[]>> predictorHolder, String text, String label)
    {
        Predictor<String, float[]> predictor = predictorHolder.get();
        if (predictor == null)
        {
            return null;
        }
        try
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
