package org.open4goods.embedding.service.image;

import java.io.IOException;
import java.nio.file.Path;

import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * DJL-backed image embedding service that exposes float vector embeddings for images.
 */
public class DjlImageEmbeddingService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DjlImageEmbeddingService.class);

    private final DjlEmbeddingProperties properties;
    private final AbstractImageModelFactory modelFactory;

    private ZooModel<Image, float[]> model;

    public DjlImageEmbeddingService(DjlEmbeddingProperties properties, AbstractImageModelFactory modelFactory)
    {
        this.properties = properties;
        this.modelFactory = modelFactory;
    }

    /**
     * Initializes the DJL model once at startup.
     *
     * @throws IOException when the model cannot be loaded.
     * @throws MalformedModelException when the model is malformed.
     * @throws ModelNotFoundException when the model cannot be resolved.
     */
    @PostConstruct
    public void initialize() throws IOException, MalformedModelException, ModelNotFoundException
    {
        String modelUrl = properties.getVisionModelUrl();
        int imageSize = properties.getImageInputSize();

        if (!StringUtils.hasText(modelUrl))
        {
            if (properties.isFailOnMissingModel())
            {
                throw new IllegalStateException("Vision embedding model URL is not configured");
            }
            LOGGER.warn("Vision embedding model URL is empty; image embeddings will be unavailable.");
            return;
        }

        LOGGER.info("Initializing DJL image embedding model from: {}", modelUrl);

        model = modelFactory.loadModel(modelUrl, imageSize);

        LOGGER.info("DJL image embedding model initialized successfully.");
    }

    /**
     * Releases model resources on shutdown.
     */
    @PreDestroy
    public void close()
    {
        if (model != null)
        {
            model.close();
        }
    }

    /**
     * Generates an embedding for the provided image path.
     *
     * @param imagePath path to the image file.
     * @return the normalized embedding vector.
     * @throws Exception when the image cannot be processed.
     */
    public float[] embed(Path imagePath) throws Exception
    {
        if (model == null)
        {
            throw new IllegalStateException("Vision embedding model is not initialized");
        }

        try (Predictor<Image, float[]> predictor = model.newPredictor())
        {
            Image img = ImageFactory.getInstance().fromFile(imagePath);
            return predictor.predict(img);
        }
        catch (TranslateException e)
        {
            LOGGER.error("Error while computing embedding for {}: {}", imagePath, e.getMessage());
            throw e;
        }
        catch (Exception e)
        {
            LOGGER.error("Error while computing embedding for {}: {}", imagePath, e.getMessage(), e);
            throw e;
        }
    }
}
