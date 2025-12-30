package org.open4goods.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embedding.service.EmbeddingModelFactory;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;

class DjlTextEmbeddingServiceTest
{
    @Test
    void embedsWithPrimaryModelWhenAvailable() throws Exception
    {
        DjlEmbeddingProperties properties = baseProperties();
        ZooModel<String, float[]> textModel = stubModel(new float[] { 0.1f, 0.2f });
        EmbeddingModelFactory factory = (location, pooling, normalize, engine) -> textModel;

        DjlTextEmbeddingService service = new DjlTextEmbeddingService(properties, factory);
        service.initialize();

        assertThat(service.embed("hello")).containsExactly(0.1f, 0.2f);
    }

    @Test
    void fallsBackToMultimodalModelWhenPrimaryFails() throws Exception
    {
        DjlEmbeddingProperties properties = baseProperties();
        properties.setTextModelUrl("text-model");
        properties.setMultimodalModelUrl("multi-model");

        ZooModel<String, float[]> multimodalModel = stubModel(new float[] { 0.3f, 0.4f });
        EmbeddingModelFactory factory = (location, pooling, normalize, engine) -> {
            if ("text-model".equals(location))
            {
                throw new IllegalStateException("text model missing");
            }
            return multimodalModel;
        };

        DjlTextEmbeddingService service = new DjlTextEmbeddingService(properties, factory);
        service.initialize();

        assertThat(service.embed("hello")).containsExactly(0.3f, 0.4f);
    }

    @Test
    void failsFastWhenNoModelCanBeLoaded()
    {
        DjlEmbeddingProperties properties = baseProperties();
        properties.setFailOnMissingModel(true);

        EmbeddingModelFactory factory = (location, pooling, normalize, engine) -> {
            throw new IllegalStateException("unavailable");
        };

        DjlTextEmbeddingService service = new DjlTextEmbeddingService(properties, factory);
        assertThatThrownBy(service::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load any DJL text embedding model");
    }

    private DjlEmbeddingProperties baseProperties()
    {
        DjlEmbeddingProperties properties = new DjlEmbeddingProperties();
        properties.setPreferLocalModels(false);
        properties.setTextModelUrl("text-model");
        properties.setMultimodalModelUrl("multimodal-model");
        properties.setFailOnMissingModel(true);
        return properties;
    }

    private ZooModel<String, float[]> stubModel(float[] vector) throws Exception
    {
        Predictor<String, float[]> predictor = mock(Predictor.class);
        when(predictor.predict(anyString())).thenReturn(vector);

        @SuppressWarnings("unchecked")
        ZooModel<String, float[]> model = mock(ZooModel.class);
        when(model.newPredictor()).thenReturn(predictor);
        return model;
    }
}
