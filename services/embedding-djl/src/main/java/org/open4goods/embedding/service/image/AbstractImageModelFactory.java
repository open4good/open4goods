package org.open4goods.embedding.service.image;

import java.io.IOException;

import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.modality.cv.Image;

/**
 * Abstract factory responsible for loading DJL image embedding models.
 */
public abstract class AbstractImageModelFactory
{
    /**
     * Loads a DJL image embedding model.
     *
     * @param modelUrl model identifier or URL.
     * @param imageSize target input size for the vision model.
     * @return a loaded {@link ZooModel}.
     * @throws IOException when the model cannot be loaded.
     * @throws MalformedModelException when the model format is invalid.
     * @throws ModelNotFoundException when the model cannot be resolved.
     */
    public abstract ZooModel<Image, float[]> loadModel(String modelUrl, int imageSize)
            throws IOException, MalformedModelException, ModelNotFoundException;
}
