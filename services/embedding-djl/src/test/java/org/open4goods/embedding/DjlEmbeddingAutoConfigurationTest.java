package org.open4goods.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.embedding.config.DjlEmbeddingAutoConfiguration;
import org.open4goods.embedding.health.DjlEmbeddingHealthIndicator;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embedding.service.EmbeddingModelFactory;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;

class DjlEmbeddingAutoConfigurationTest
{
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("embedding.prefer-local-models=false",
                    "embedding.text-model-url=text-model",
                    "embedding.multimodal-model-url=multi-model")
            .withConfiguration(AutoConfigurations.of(DjlEmbeddingAutoConfiguration.class));

    @Test
    void autoConfigurationRegistersServiceAndHealthIndicator()
    {
        contextRunner.withBean(EmbeddingModelFactory.class, () -> new StubFactory())
                .run(context -> {
                    assertThat(context).hasSingleBean(DjlTextEmbeddingService.class);
                    assertThat(context).hasSingleBean(DjlEmbeddingHealthIndicator.class);

                    DjlEmbeddingHealthIndicator indicator = context.getBean(DjlEmbeddingHealthIndicator.class);
                    assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
                });
    }

    private static class StubFactory implements EmbeddingModelFactory
    {
        @Override
        public ZooModel<String, float[]> loadModel(String modelLocation, String poolingMode, boolean normalize, String engine)
                throws Exception
        {
            return buildModel(new float[] { 0.9f, 0.1f });
        }

        private ZooModel<String, float[]> buildModel(float[] vector) throws Exception
        {
            Predictor<String, float[]> predictor = org.mockito.Mockito.mock(Predictor.class);
            org.mockito.Mockito.when(predictor.predict(org.mockito.ArgumentMatchers.anyString())).thenReturn(vector);

            @SuppressWarnings("unchecked")
            ZooModel<String, float[]> model = org.mockito.Mockito.mock(ZooModel.class);
            org.mockito.Mockito.when(model.newPredictor()).thenReturn(predictor);
            return model;
        }
    }
}
