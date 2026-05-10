package org.open4goods.embedding.service;

import java.util.List;

/**
 * Computes dense text embeddings used by semantic product search.
 */
public interface TextEmbeddingService
{
    /**
     * Embeds one text payload.
     *
     * @param text text to embed
     * @return embedding vector
     */
    float[] embed(String text);

    /**
     * Embeds several text payloads while preserving input order.
     *
     * @param texts texts to embed
     * @return embedding vectors in input order
     */
    List<float[]> embedBatch(List<String> texts);
}
