package org.open4goods.embeddinggateway.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for image embeddings based on a remote URL.
 */
public record ImageEmbeddingRequest(@NotBlank String url)
{
}
