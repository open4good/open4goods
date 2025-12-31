package org.open4goods.commons.services;

/**
 * Simple contract for components able to generate embedding vectors for free text inputs.
 */
public interface TextEmbeddingService
{

    /**
     * Generates an embedding vector for the provided text.
     *
     * @param text the text to embed
     * @return a normalised embedding vector or {@code null} when the text cannot be embedded
     */
    float[] embed(String text);
}
