package org.open4goods.api.services.completion.image;

import static org.assertj.core.api.Assertions.assertThat;

import ai.djl.Model;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.metric.Metrics;
import ai.djl.translate.TranslatorContext;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class EmbeddingTranslatorTest {

    @Test
    void processInputProducesNormalizedCHWTensor() {
        EmbeddingTranslator translator = new EmbeddingTranslator(2);

        BufferedImage bufferedImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        bufferedImage.setRGB(0, 0, new Color(255, 0, 0).getRGB());
        bufferedImage.setRGB(1, 0, new Color(0, 255, 0).getRGB());
        bufferedImage.setRGB(0, 1, new Color(0, 0, 255).getRGB());
        bufferedImage.setRGB(1, 1, new Color(255, 255, 255).getRGB());

        ai.djl.modality.cv.Image djlImage = ImageFactory.getInstance().fromImage(bufferedImage);

        try (MockTranslatorContext context = new MockTranslatorContext()) {
            NDList ndList = translator.processInput(context, djlImage);
            NDArray array = ndList.singletonOrThrow();

            assertThat(array.getShape()).isEqualTo(new Shape(1, 3, 2, 2));

            float[] data = array.toFloatArray();
            assertThat(data).hasSize(12);

            float[] expected = new float[]{
                    normalize(1f, 0.485f, 0.229f),
                    normalize(0f, 0.485f, 0.229f),
                    normalize(0f, 0.485f, 0.229f),
                    normalize(1f, 0.485f, 0.229f),
                    normalize(0f, 0.456f, 0.224f),
                    normalize(1f, 0.456f, 0.224f),
                    normalize(0f, 0.456f, 0.224f),
                    normalize(1f, 0.456f, 0.224f),
                    normalize(0f, 0.406f, 0.225f),
                    normalize(0f, 0.406f, 0.225f),
                    normalize(1f, 0.406f, 0.225f),
                    normalize(1f, 0.406f, 0.225f)
            };

            assertThat(data).containsExactly(expected);
        }
    }

    private static float normalize(float value, float mean, float std) {
        return (value - mean) / std;
    }

    private static class MockTranslatorContext implements TranslatorContext {

        private final NDManager manager = NDManager.newBaseManager();

        @Override
        public Model getModel() {
            return null;
        }

        @Override
        public NDManager getNDManager() {
            return manager;
        }

        @Override
        public NDManager getPredictorManager() {
            return manager;
        }

        @Override
        public Block getBlock() {
            return null;
        }

        @Override
        public Metrics getMetrics() {
            return null;
        }

        @Override
        public Object getAttachment(String key) {
            return null;
        }

        @Override
        public void setAttachment(String key, Object value) {
            // no-op
        }

        @Override
        public void close() {
            manager.close();
        }
    }
}
