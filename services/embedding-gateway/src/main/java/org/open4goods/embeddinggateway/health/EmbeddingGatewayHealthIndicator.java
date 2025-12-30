package org.open4goods.embeddinggateway.health;

import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embeddinggateway.service.ImageEmbeddingService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator exposing the readiness of text and image embedding models.
 */
@Component
public class EmbeddingGatewayHealthIndicator implements HealthIndicator
{
    private final DjlTextEmbeddingService textEmbeddingService;
    private final ImageEmbeddingService imageEmbeddingService;

    public EmbeddingGatewayHealthIndicator(DjlTextEmbeddingService textEmbeddingService,
            ImageEmbeddingService imageEmbeddingService)
    {
        this.textEmbeddingService = textEmbeddingService;
        this.imageEmbeddingService = imageEmbeddingService;
    }

    @Override
    public Health health()
    {
        Health.Builder builder = Health.up();
        builder.withDetail("textModelLoaded", textEmbeddingService.isTextModelLoaded());
        builder.withDetail("textModel", textEmbeddingService.getTextModelLocation().orElse("unknown"));
        builder.withDetail("multimodalModelLoaded", textEmbeddingService.isMultimodalTextModelLoaded());
        builder.withDetail("multimodalModel",
                textEmbeddingService.getMultimodalModelLocation().orElse("unknown"));
        builder.withDetail("imageModelLoaded", imageEmbeddingService.isReady());
        return builder.build();
    }
}
