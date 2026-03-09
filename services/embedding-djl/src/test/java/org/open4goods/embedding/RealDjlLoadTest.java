package org.open4goods.embedding;

import org.junit.jupiter.api.Test;
import org.open4goods.embedding.config.DjlEmbeddingAutoConfiguration;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.DefaultTextModelFactory;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealDjlLoadTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "embedding.text-model-url=djl://ai.djl.huggingface.pytorch/intfloat/multilingual-e5-small",
                    "embedding.vision-model-url=djl://ai.djl.pytorch/resnet18_embedding/0.0.1",
                    "embedding.async-loading=true",
                    "embedding.predictor-pool-size=1")
            .withConfiguration(AutoConfigurations.of(DjlEmbeddingAutoConfiguration.class));

    @Test
    public void testRealModelLoad() {
        System.out.println("Starting real model load test with async = true...");
        contextRunner.run(context -> {
            DjlTextEmbeddingService service = context.getBean(DjlTextEmbeddingService.class);
            service.awaitReady();
            System.out.println("Text Embedding ready: " + service.isReady());
            
            org.open4goods.embedding.service.image.DjlImageEmbeddingService imgService = context.getBean(org.open4goods.embedding.service.image.DjlImageEmbeddingService.class);
            imgService.awaitReady();
            System.out.println("Image Embedding ready: " + imgService.isReady());
        });
    }
}
