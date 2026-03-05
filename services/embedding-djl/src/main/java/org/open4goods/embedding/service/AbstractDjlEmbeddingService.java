package org.open4goods.embedding.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import ai.djl.repository.zoo.ZooModel;
import jakarta.annotation.PreDestroy;

/**
 * Base class handling DJL model lifecycle and error reporting for embedding services.
 * <p>
 * Models can be loaded eagerly or asynchronously (controlled by {@code embedding.async-loading}).
 * {@link Predictor} instances are managed through a bounded pool
 * ({@link BlockingQueue}) to keep native memory usage predictable and to work
 * correctly with both platform threads and virtual threads (unlike {@code ThreadLocal}).
 * </p>
 * <p>
 * This class also provides a {@link #embedBatch(List)} method for batching multiple
 * texts in a single call, improving throughput by reducing JNI/predictor overhead.
 * </p>
 */
public abstract class AbstractDjlEmbeddingService implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDjlEmbeddingService.class);

    /** Maximum time (seconds) to wait for a predictor from the pool. */
    private static final int POOL_ACQUIRE_TIMEOUT_SECONDS = 30;

    private final DjlEmbeddingProperties properties;
    private final AbstractTextModelFactory modelFactory;

    private volatile ZooModel<String, float[]> textModel;
    private volatile ZooModel<String, float[]> visionModel;
    private volatile String resolvedTextModelLocation;
    private volatile String resolvedVisionModelLocation;

    /** Bounded predictor pools: one per model type. */
    private volatile BlockingQueue<Predictor<String, float[]>> textPredictorPool;
    private volatile BlockingQueue<Predictor<String, float[]>> visionPredictorPool;

    private final AtomicBoolean ready = new AtomicBoolean(false);
    private CompletableFuture<Void> initFuture;

    protected AbstractDjlEmbeddingService(DjlEmbeddingProperties properties, AbstractTextModelFactory modelFactory)
    {
        this.properties = properties;
        this.modelFactory = modelFactory;

        if (properties.isAsyncLoading())
        {
            LOGGER.info("DJL embedding: async model loading enabled");
            initFuture = CompletableFuture.runAsync(this::loadModels);
        }
        else
        {
            loadModels();
        }
    }

    // ------------------------------------------------------------------ loading

    private void loadModels()
    {
        resolvedTextModelLocation = properties.getTextModelUrl();
        resolvedVisionModelLocation = properties.getVisionModelUrl();

        textModel = tryLoad(resolvedTextModelLocation, "text");
        visionModel = tryLoad(resolvedVisionModelLocation, "multimodal");

        if (textModel == null && visionModel == null && properties.isFailOnMissingModel())
        {
            throw new IllegalStateException("Failed to load any DJL text embedding model (text or multimodal)");
        }

        int poolSize = properties.getPredictorPoolSize();
        textPredictorPool = createPool(textModel, poolSize, "text");
        visionPredictorPool = createPool(visionModel, poolSize, "multimodal");

        ready.set(true);

        LOGGER.info("DJL embedding initialised. textModelLoaded={}, multimodalLoaded={}, poolSize={}, dimension={}.",
                textModel != null, visionModel != null, poolSize, properties.getEmbeddingDimension());
    }

    private BlockingQueue<Predictor<String, float[]>> createPool(
            ZooModel<String, float[]> model, int poolSize, String label)
    {
        if (model == null)
        {
            return null;
        }
        BlockingQueue<Predictor<String, float[]>> pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++)
        {
            pool.add(model.newPredictor());
        }
        LOGGER.info("Created {} predictor pool with {} instances", label, poolSize);
        return pool;
    }

    // ------------------------------------------------------------------ public API

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
        ensureReady();

        float[] vector = embedWithPool(textPredictorPool, text, "text");
        if (vector != null)
        {
            return vector;
        }
        vector = embedWithPool(visionPredictorPool, text, "multimodal");
        if (vector != null)
        {
            return vector;
        }
        throw new IllegalStateException("No embedding model available to process the requested text");
    }

    /**
     * Embeds multiple texts in a single call, improving throughput by reducing
     * per-call predictor acquisition overhead.
     *
     * @param texts the list of texts to embed, must not be null or empty
     * @return list of embedding vectors in the same order as the input
     * @throws IllegalArgumentException if the list is null or empty
     * @throws IllegalStateException    if no model is available
     */
    public List<float[]> embedBatch(List<String> texts)
    {
        if (texts == null || texts.isEmpty())
        {
            throw new IllegalArgumentException("Text list to embed must not be null or empty");
        }
        ensureReady();

        // Try with the text predictor pool first
        List<float[]> results = embedBatchWithPool(textPredictorPool, texts, "text");
        if (results != null)
        {
            return results;
        }
        // Fallback to multimodal
        results = embedBatchWithPool(visionPredictorPool, texts, "multimodal");
        if (results != null)
        {
            return results;
        }
        throw new IllegalStateException("No embedding model available to process the requested texts");
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

    /**
     * Returns {@code true} once all models have been loaded (or failed to load).
     */
    public boolean isReady()
    {
        return ready.get();
    }

    // ------------------------------------------------------------------ lifecycle

    @PreDestroy
    @Override
    public void close()
    {
        drainAndClosePool(textPredictorPool);
        drainAndClosePool(visionPredictorPool);
        closeQuietly(textModel);
        closeQuietly(visionModel);
    }

    // ------------------------------------------------------------------ internals

    private void ensureReady()
    {
        if (!ready.get())
        {
            awaitReady();
        }
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
     * Borrows a {@link Predictor} from the pool, runs a single prediction, then returns it.
     */
    private float[] embedWithPool(BlockingQueue<Predictor<String, float[]>> pool, String text, String label)
    {
        if (pool == null)
        {
            return null;
        }
        Predictor<String, float[]> predictor = null;
        try
        {
            predictor = pool.poll(POOL_ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (predictor == null)
            {
                LOGGER.error("Timed out waiting for a {} predictor from the pool", label);
                return null;
            }
            return predictor.predict(text);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while waiting for a {} predictor from the pool", label);
            return null;
        }
        catch (Exception e)
        {
            LOGGER.error("Error embedding text with {} model", label, e);
            return null;
        }
        finally
        {
            if (predictor != null)
            {
                pool.offer(predictor);
            }
        }
    }

    /**
     * Borrows a {@link Predictor} from the pool, processes a batch of texts, then returns it.
     * The predictor is held for the duration of the entire batch to avoid repeated pool contention.
     */
    private List<float[]> embedBatchWithPool(BlockingQueue<Predictor<String, float[]>> pool,
            List<String> texts, String label)
    {
        if (pool == null)
        {
            return null;
        }
        Predictor<String, float[]> predictor = null;
        try
        {
            predictor = pool.poll(POOL_ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (predictor == null)
            {
                LOGGER.error("Timed out waiting for a {} predictor from the pool for batch", label);
                return null;
            }
            List<float[]> results = new ArrayList<>(texts.size());
            for (String text : texts)
            {
                if (!StringUtils.hasText(text))
                {
                    results.add(null);
                }
                else
                {
                    results.add(predictor.predict(text));
                }
            }
            return results;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while waiting for a {} predictor from the pool for batch", label);
            return null;
        }
        catch (Exception e)
        {
            LOGGER.error("Error embedding text batch with {} model", label, e);
            return null;
        }
        finally
        {
            if (predictor != null)
            {
                pool.offer(predictor);
            }
        }
    }

    private void drainAndClosePool(BlockingQueue<Predictor<String, float[]>> pool)
    {
        if (pool == null)
        {
            return;
        }
        Predictor<String, float[]> predictor;
        while ((predictor = pool.poll()) != null)
        {
            try
            {
                predictor.close();
            }
            catch (Exception e)
            {
                LOGGER.warn("Error closing predictor", e);
            }
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
