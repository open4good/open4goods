package org.open4goods.embedding.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.open4goods.commons.services.TextEmbeddingService;
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
public abstract class AbstractDjlEmbeddingService implements TextEmbeddingService, AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDjlEmbeddingService.class);

    private final DjlEmbeddingProperties properties;
    private final EmbeddingModelFactory modelFactory;

    private ZooModel<String, float[]> textModel;
    private ZooModel<String, float[]> multimodalTextModel;
    private String resolvedTextModelLocation;
    private String resolvedMultimodalModelLocation;

    protected AbstractDjlEmbeddingService(DjlEmbeddingProperties properties, EmbeddingModelFactory modelFactory)
    {
        this.properties = properties;
        this.modelFactory = modelFactory;
    }

    @PostConstruct
    public void initialize()
    {
        resolvedTextModelLocation = resolveModelLocation(properties.getTextModelPath(), properties.getTextModelUrl(), "text");
        resolvedMultimodalModelLocation = resolveModelLocation(properties.getMultimodalModelPath(),
                properties.getMultimodalModelUrl(), "multimodal");

        textModel = tryLoad(resolvedTextModelLocation, "text");
        multimodalTextModel = tryLoad(resolvedMultimodalModelLocation, "multimodal");

        if (textModel == null && multimodalTextModel == null && properties.isFailOnMissingModel())
        {
            throw new IllegalStateException("Failed to load any DJL text embedding model (text or multimodal)");
        }
        LOGGER.info("DJL embedding initialised. textModelLoaded={}, multimodalLoaded={}, dimension={}.",
                textModel != null, multimodalTextModel != null, properties.getEmbeddingDimension());
    }

    @Override
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
        vector = embedWithModel(multimodalTextModel, text, "multimodal");
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
        return multimodalTextModel != null;
    }

    @PreDestroy
    @Override
    public void close()
    {
        closeQuietly(textModel);
        closeQuietly(multimodalTextModel);
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
            return modelFactory.loadModel(modelLocation, properties.getPoolingMode(), properties.isNormalizeOutputs(),
                    properties.getEngine());
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to initialize {} embedding model from {}", label, modelLocation, e);
            return null;
        }
    }

    private String resolveModelLocation(String localPath, String remoteUrl, String label)
    {
        if (properties.isPreferLocalModels() && StringUtils.hasText(localPath))
        {
            Path path = Path.of(localPath);
            if (Files.exists(path))
            {
                LOGGER.info("Using local {} embedding model at {}", label, path.toAbsolutePath());
                return path.toUri().toString();
            }
            String message = "Local " + label + " embedding model not found at " + path.toAbsolutePath();
            if (properties.isFailOnMissingModel() && !StringUtils.hasText(remoteUrl))
            {
                throw new IllegalStateException(message + " and no remote URL configured");
            }
            LOGGER.warn("{}. Falling back to remote URL {}", message, remoteUrl);
        }
        return remoteUrl;
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
