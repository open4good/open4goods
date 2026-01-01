package org.open4goods.nudgerfrontapi.config;

import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.AbstractTextModelFactory;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embedding.service.image.AbstractImageModelFactory;
import org.open4goods.embedding.service.image.DjlImageEmbeddingService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Test-only configuration supplying a lightweight {@link DjlTextEmbeddingService}
 * so integration tests do not depend on external DJL model downloads.
 */
@Configuration
public class TestTextEmbeddingConfig
{
    @Bean
    @Primary
    DjlTextEmbeddingService testTextEmbeddingService()
    {
        DjlEmbeddingProperties properties = new DjlEmbeddingProperties();
        properties.setTextModelUrl("test-text-model");
        properties.setMultimodalModelUrl("test-multimodal-model");
        properties.setFailOnMissingModel(true);

        AbstractTextModelFactory factory = new AbstractTextModelFactory()
        {
            @Override
            public ai.djl.repository.zoo.ZooModel<String, float[]> loadModel(String modelLocation, String poolingMode, String engine)
                    throws Exception
            {
                ai.djl.inference.Predictor<String, float[]> predictor = Mockito.mock(ai.djl.inference.Predictor.class);
                Mockito.when(predictor.predict(Mockito.anyString())).thenReturn(new float[] { 0.0f, 0.0f });

                @SuppressWarnings("unchecked")
                ai.djl.repository.zoo.ZooModel<String, float[]> model = Mockito.mock(ai.djl.repository.zoo.ZooModel.class);
                Mockito.when(model.newPredictor()).thenReturn(predictor);
                return model;
            }
        };

        DjlTextEmbeddingService service = new DjlTextEmbeddingService(properties, factory);
        service.initialize();
        return service;
    }

    @Bean
    @Primary
    DjlImageEmbeddingService testImageEmbeddingService()
    {
        DjlEmbeddingProperties properties = new DjlEmbeddingProperties();
        properties.setVisionModelUrl("test-vision-model");
        properties.setFailOnMissingModel(true);

        AbstractImageModelFactory factory = new AbstractImageModelFactory()
        {
            @Override
            public ai.djl.repository.zoo.ZooModel<ai.djl.modality.cv.Image, float[]> loadModel(String modelUrl, int imageSize)
            {
                ai.djl.inference.Predictor<ai.djl.modality.cv.Image, float[]> predictor =
                        Mockito.mock(ai.djl.inference.Predictor.class);
                Mockito.when(predictor.predict(Mockito.any(ai.djl.modality.cv.Image.class)))
                        .thenReturn(new float[] { 0.0f, 0.0f });

                @SuppressWarnings("unchecked")
                ai.djl.repository.zoo.ZooModel<ai.djl.modality.cv.Image, float[]> model =
                        Mockito.mock(ai.djl.repository.zoo.ZooModel.class);
                Mockito.when(model.newPredictor()).thenReturn(predictor);
                return model;
            }
        };

        DjlImageEmbeddingService service = new DjlImageEmbeddingService(properties, factory);
        service.initialize();
        return service;
    }
}
