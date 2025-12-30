package org.open4goods.embedding.service;

import org.open4goods.embedding.config.DjlEmbeddingProperties;

/**
 * DJL-backed {@link org.open4goods.commons.services.TextEmbeddingService} for text inputs.
 */
public class DjlTextEmbeddingService extends AbstractDjlEmbeddingService
{
    public DjlTextEmbeddingService(DjlEmbeddingProperties properties, EmbeddingModelFactory modelFactory)
    {
        super(properties, modelFactory);
    }
}
