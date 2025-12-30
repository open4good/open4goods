package org.open4goods.embeddinggateway.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for text embeddings.
 */
public record TextEmbeddingRequest(@NotBlank String text)
{
}
