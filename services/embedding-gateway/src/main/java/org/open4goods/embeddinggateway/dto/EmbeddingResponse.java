package org.open4goods.embeddinggateway.dto;

/**
 * Simple response payload wrapping an embedding vector.
 */
public record EmbeddingResponse(float[] embedding)
{
}
