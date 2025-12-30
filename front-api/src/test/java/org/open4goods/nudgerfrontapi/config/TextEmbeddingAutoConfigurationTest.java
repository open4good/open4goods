package org.open4goods.nudgerfrontapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.services.TextEmbeddingService;
import org.open4goods.nudgerfrontapi.service.http.EmbeddingGatewayClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TextEmbeddingAutoConfigurationTest
{
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmbeddingClientConfig.class))
            .withPropertyValues(
                    "front.embedding-client.base-url=http://localhost:9999",
                    "embedding.enabled=false");

    @Test
    void embeddingGatewayClientIsExposed()
    {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TextEmbeddingService.class);
            assertThat(context.getBean(TextEmbeddingService.class)).isInstanceOf(EmbeddingGatewayClient.class);
        });
    }
}
