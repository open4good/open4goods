package org.open4goods.embeddinggateway.service;

import java.net.URL;

/**
 * Contract for gateway image embedding operations.
 */
public interface ImageEmbeddingService
{
    /**
     * Computes an embedding for the remote image referenced by the given URL.
     *
     * @param imageUrl image URL to fetch
     * @return normalised embedding vector
     * @throws Exception when the image cannot be fetched or processed
     */
    float[] embed(URL imageUrl) throws Exception;

    /**
     * Indicates whether the underlying model has been loaded.
     *
     * @return {@code true} when the model is ready to serve predictions
     */
    boolean isReady();
}
