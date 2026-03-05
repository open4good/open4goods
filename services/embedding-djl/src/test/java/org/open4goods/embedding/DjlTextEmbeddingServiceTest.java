package org.open4goods.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.AbstractTextModelFactory;
import org.open4goods.embedding.service.DjlTextEmbeddingService;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;

class DjlTextEmbeddingServiceTest
{
    @Test
    @SuppressWarnings("resource")
    void embedsWithPrimaryModelWhenAvailable() throws Exception
    {
        DjlEmbeddingProperties properties = baseProperties();
        ZooModel<String, float[]> textModel = stubModel(new float[] { 0.1f, 0.2f });
        AbstractTextModelFactory factory = new StubFactory(textModel);

        DjlTextEmbeddingService service = new DjlTextEmbeddingService(properties, factory);

        assertThat(service.embed("hello")).containsExactly(0.1f, 0.2f);
    }

    @Test
    @SuppressWarnings("resource")
    void fallsBackToMultimodalModelWhenPrimaryFails() throws Exception
    {
        DjlEmbeddingProperties properties = baseProperties();
        properties.setTextModelUrl("text-model");
        properties.setVisionModelUrl("multi-model");

        ZooModel<String, float[]> multimodalModel = stubModel(new float[] { 0.3f, 0.4f });
        AbstractTextModelFactory factory = new AbstractTextModelFactory()
        {
            @Override
            public ZooModel<String, float[]> loadModel(String modelLocation, String poolingMode, String engine)
            {
                if ("text-model".equals(modelLocation))
                {
                    throw new IllegalStateException("text model missing");
                }
                return multimodalModel;
            }
        };

        DjlTextEmbeddingService service = new DjlTextEmbeddingService(properties, factory);

        assertThat(service.embed("hello")).containsExactly(0.3f, 0.4f);
    }

    @Test
    void failsFastWhenNoModelCanBeLoaded()
    {
        DjlEmbeddingProperties properties = baseProperties();
        properties.setFailOnMissingModel(true);

        AbstractTextModelFactory factory = new AbstractTextModelFactory()
        {
            @Override
            public ZooModel<String, float[]> loadModel(String modelLocation, String poolingMode, String engine)
            {
                throw new IllegalStateException("unavailable");
            }
        };

        assertThatThrownBy(() -> new DjlTextEmbeddingService(properties, factory))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load any DJL text embedding model");
    }

    @Test
    @SuppressWarnings("resource")
    void embedBatchReturnsVectorsInOrder() throws Exception
    {
        DjlEmbeddingProperties properties = baseProperties();
        // Predictor that returns a vector derived from the input length
        Predictor<String, float[]> predictor = mock(Predictor.class);
        when(predictor.predict(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            return new float[] { (float) text.length() };
        });

        @SuppressWarnings("unchecked")
        ZooModel<String, float[]> textModel = mock(ZooModel.class);
        when(textModel.newPredictor()).thenReturn(predictor);

        AbstractTextModelFactory factory = new StubFactory(textModel);

        DjlTextEmbeddingService service = new DjlTextEmbeddingService(properties, factory);

        List<float[]> results = service.embedBatch(List.of("ab", "abcd", "a"));

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).containsExactly(2.0f);
        assertThat(results.get(1)).containsExactly(4.0f);
        assertThat(results.get(2)).containsExactly(1.0f);
    }

    @Test
    @SuppressWarnings("resource")
    void isReadyReturnsTrueAfterSynchronousLoad() throws Exception
    {
        DjlEmbeddingProperties properties = baseProperties();
        ZooModel<String, float[]> textModel = stubModel(new float[] { 1.0f });
        AbstractTextModelFactory factory = new StubFactory(textModel);

        DjlTextEmbeddingService service = new DjlTextEmbeddingService(properties, factory);

        assertThat(service.isReady()).isTrue();
    }

    private DjlEmbeddingProperties baseProperties()
    {
        DjlEmbeddingProperties properties = new DjlEmbeddingProperties();
        properties.setTextModelUrl("text-model");
        properties.setVisionModelUrl("multimodal-model");
        properties.setFailOnMissingModel(true);
        // Synchronous loading in tests for predictability
        properties.setAsyncLoading(false);
        // Small pool for tests
        properties.setPredictorPoolSize(1);
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

    private static class StubFactory extends AbstractTextModelFactory
    {
        private final ZooModel<String, float[]> model;

        private StubFactory(ZooModel<String, float[]> model)
        {
            this.model = model;
        }

        @Override
        public ZooModel<String, float[]> loadModel(String modelLocation, String poolingMode, String engine)
        {
            return model;
        }
    }
}
