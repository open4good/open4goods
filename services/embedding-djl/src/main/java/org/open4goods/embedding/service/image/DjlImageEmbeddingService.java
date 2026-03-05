package org.open4goods.embedding.service.image;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * DJL-backed image embedding service that exposes float vector embeddings for images.
 * <p>
 * Uses a bounded {@link BlockingQueue} pool of predictors instead of creating a new
 * predictor per request, keeping native memory usage predictable.
 * </p>
 */
public class DjlImageEmbeddingService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DjlImageEmbeddingService.class);

    /** Maximum time (seconds) to wait for a predictor from the pool. */
    private static final int POOL_ACQUIRE_TIMEOUT_SECONDS = 30;

    private final DjlEmbeddingProperties properties;
    private final AbstractImageModelFactory modelFactory;

    private volatile ZooModel<Image, float[]> model;
    private volatile BlockingQueue<Predictor<Image, float[]>> predictorPool;

    private final AtomicBoolean ready = new AtomicBoolean(false);
    private CompletableFuture<Void> initFuture;

    public DjlImageEmbeddingService(DjlEmbeddingProperties properties, AbstractImageModelFactory modelFactory)
    {
        this.properties = properties;
        this.modelFactory = modelFactory;
    }

    /**
     * Initializes the DJL model once at startup, either synchronously or asynchronously.
     * @throws Exception if synchronous loading fails
     */
    @PostConstruct
    public void initialize() throws Exception
    {
        if (properties.isAsyncLoading())
        {
            LOGGER.info("DJL image embedding: async model loading enabled");
            initFuture = CompletableFuture.runAsync(this::doInitialize);
        }
        else
        {
            doInitialize();
        }
    }

    private void doInitialize()
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
            ready.set(true);
            return;
        }

        LOGGER.info("Initializing DJL image embedding model from: {}", modelUrl);

        try
        {
            model = modelFactory.loadModel(modelUrl, imageSize);

            int poolSize = properties.getPredictorPoolSize();
            predictorPool = new ArrayBlockingQueue<>(poolSize);
            for (int i = 0; i < poolSize; i++)
            {
                predictorPool.add(model.newPredictor());
            }

            LOGGER.info("DJL image embedding model initialized with predictor pool of size {}", poolSize);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to initialize image embedding model from {}", modelUrl, e);
            if (properties.isFailOnMissingModel())
            {
                throw new IllegalStateException("Failed to load image embedding model", e);
            }
        }
        finally
        {
            ready.set(true);
        }
    }

    /**
     * Blocks until async model loading is complete. No-op if loading was synchronous.
     */
    public void awaitReady()
    {
        if (initFuture != null)
        {
            initFuture.join();
        }
    }

    /**
     * Returns {@code true} once the model has been loaded (or has failed to load).
     */
    public boolean isReady()
    {
        return ready.get();
    }

    /**
     * Releases model resources on shutdown.
     */
    @PreDestroy
    public void close()
    {
        drainAndClosePool();
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
        if (!ready.get())
        {
            awaitReady();
        }

        if (model == null || predictorPool == null)
        {
            throw new IllegalStateException("Vision embedding model is not initialized");
        }

        Predictor<Image, float[]> predictor = null;
        try
        {
            predictor = predictorPool.poll(POOL_ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (predictor == null)
            {
                throw new IllegalStateException("Timed out waiting for an image predictor from the pool");
            }
            Image img = ImageFactory.getInstance().fromFile(imagePath);
            return predictor.predict(img);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an image predictor", e);
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
        finally
        {
            if (predictor != null)
            {
                predictorPool.offer(predictor);
            }
        }
    }

    private void drainAndClosePool()
    {
        if (predictorPool == null)
        {
            return;
        }
        Predictor<Image, float[]> predictor;
        while ((predictor = predictorPool.poll()) != null)
        {
            try
            {
                predictor.close();
            }
            catch (Exception e)
            {
                LOGGER.warn("Error closing image predictor", e);
            }
        }
    }
}
