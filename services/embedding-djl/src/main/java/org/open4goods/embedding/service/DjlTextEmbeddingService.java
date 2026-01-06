package org.open4goods.embedding.service;

import org.open4goods.embedding.config.DjlEmbeddingProperties;

/**
 * DJL-backed service for text embedding inputs.
 */
public class DjlTextEmbeddingService extends AbstractDjlEmbeddingService
{
    public DjlTextEmbeddingService(DjlEmbeddingProperties properties, AbstractTextModelFactory modelFactory)
    {
        super(properties, modelFactory);
    }
}
