package org.open4goods.api.services.completion.image;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.transform.CenterCrop;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

/**
 * Translator that converts an {@link Image} into a normalized embedding vector
 * using a pre-trained model.
 */
public class EmbeddingTranslator implements Translator<Image, float[]> {

    private final int imageSize;
    private final Pipeline pipeline;

    public EmbeddingTranslator(int imageSize) {
        this.imageSize = imageSize;

        this.pipeline = new Pipeline()
                .add(new Resize(imageSize))
                .add(new CenterCrop(imageSize, imageSize))
                .add(new ToTensor())
                .add(new Normalize(
                        new float[]{0.485f, 0.456f, 0.406f}, // mean
                        new float[]{0.229f, 0.224f, 0.225f}  // std
                ));
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        // Use a *separate* manager for preprocessing so we can use full NDArray ops
        try (NDManager tmp = NDManager.newBaseManager()) {

            // 1. Image -> NDArray on tmp manager
            NDArray array = input.toNDArray(tmp); // shape: [H, W, C] uint8

            // 2. Run pipeline (resize, crop, ToTensor, Normalize)
            NDList processed = pipeline.transform(new NDList(array));
            NDArray img = processed.singletonOrThrow(); // [C, H, W] float

            // 3. Add batch dimension in *Java*, then create NDArray on model's manager
            Shape s = img.getShape();           // [C, H, W]
            long c = s.get(0);
            long h = s.get(1);
            long w = s.get(2);

            // Flatten data
            float[] data = img.toFloatArray();  // length = C*H*W

            // New shape [1, C, H, W]
            Shape batchedShape = new Shape(1, c, h, w);

            // 4. Create final NDArray in the model engine manager
            NDManager modelManager = ctx.getNDManager();
            NDArray modelInput = modelManager.create(data, batchedShape);

            return new NDList(modelInput);
        }
    }

    @Override
    public float[] processOutput(TranslatorContext ctx, NDList list) {
        NDArray embedding = list.get(0); // shape: [1, D] or [D]

        // Convert to Java array directly; no ND ops (norm/div) on this engine
        float[] vec = embedding.toFloatArray(); // becomes flat [D] or [1*D]

        // L2-normalize in plain Java
        double sumSq = 0.0;
        for (float v : vec) {
            sumSq += (double) v * v;
        }
        double norm = Math.sqrt(sumSq) + 1e-12;

        for (int i = 0; i < vec.length; i++) {
            vec[i] = (float) (vec[i] / norm);
        }

        return vec; // normalized embedding
    }

    @Override
    public Batchifier getBatchifier() {
        // Single image at a time; we handle batch dim ourselves.
        return null;
    }
}
