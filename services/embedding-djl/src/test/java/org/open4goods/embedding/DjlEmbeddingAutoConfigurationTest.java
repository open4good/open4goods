package org.open4goods.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.embedding.config.DjlEmbeddingAutoConfiguration;
import org.open4goods.embedding.service.image.AbstractImageModelFactory;
import org.open4goods.embedding.service.image.DjlImageEmbeddingService;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

/**
 * Verifies that the starter registers image embedding only.
 */
class DjlEmbeddingAutoConfigurationTest
{
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "embedding.vision-model-url=image-model",
                    "embedding.async-loading=false",
                    "embedding.predictor-pool-size=1")
            .withConfiguration(AutoConfigurations.of(DjlEmbeddingAutoConfiguration.class));

    /**
     * Verifies image embedding remains available after text embedding removal.
     */
    @Test
    void autoConfigurationRegistersImageServiceOnly()
    {
        contextRunner.withBean(AbstractImageModelFactory.class, StubImageFactory::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(DjlImageEmbeddingService.class);
                    assertThat(context.getBeansOfType(AbstractImageModelFactory.class)).hasSize(1);
                });
    }

    /**
     * Verifies the starter can be disabled without registering image infrastructure.
     */
    @Test
    void autoConfigurationCanBeDisabled()
    {
        contextRunner.withPropertyValues("embedding.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DjlImageEmbeddingService.class);
                    assertThat(context).doesNotHaveBean(AbstractImageModelFactory.class);
                });
    }

    /**
     * Supplies an offline image model to the application context.
     */
    private static class StubImageFactory extends AbstractImageModelFactory
    {
        @Override
        public ZooModel<Image, float[]> loadModel(String modelUrl, int imageSize) throws TranslateException
        {
            Predictor<Image, float[]> predictor = org.mockito.Mockito.mock(Predictor.class);
            org.mockito.Mockito.when(predictor.predict(org.mockito.ArgumentMatchers.any(Image.class)))
                    .thenReturn(new float[] { 0.1f, 0.2f });

            @SuppressWarnings("unchecked")
            ZooModel<Image, float[]> model = org.mockito.Mockito.mock(ZooModel.class);
            org.mockito.Mockito.when(model.newPredictor()).thenReturn(predictor);
            return model;
        }
    }
}
