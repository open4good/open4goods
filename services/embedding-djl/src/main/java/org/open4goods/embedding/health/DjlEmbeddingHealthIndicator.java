package org.open4goods.embedding.health;

import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Reports the availability of DJL text embedding models.
 */
public class DjlEmbeddingHealthIndicator implements HealthIndicator
{
    private final DjlTextEmbeddingService embeddingService;

    public DjlEmbeddingHealthIndicator(DjlTextEmbeddingService embeddingService)
    {
        this.embeddingService = embeddingService;
    }

    @Override
    public Health health()
    {
        boolean textLoaded = embeddingService.isTextModelLoaded();
        boolean multimodalLoaded = embeddingService.isMultimodalTextModelLoaded();

        Health.Builder builder = (textLoaded || multimodalLoaded) ? Health.up() : Health.down();
        embeddingService.getTextModelLocation().ifPresent(location -> builder.withDetail("textModel.location", location));
        builder.withDetail("textModel.loaded", textLoaded);

        embeddingService.getMultimodalModelLocation()
                .ifPresent(location -> builder.withDetail("multimodalModel.location", location));
        builder.withDetail("multimodalModel.loaded", multimodalLoaded);

        return builder.build();
    }
}
