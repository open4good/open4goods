package org.open4goods.embedding.service;

import ai.djl.repository.zoo.ZooModel;

/**
 * Abstract factory responsible for loading DJL text embedding models.
 */
public abstract class AbstractTextModelFactory
{
    /**
     * Loads a DJL model for the provided location.
     *
     * @param modelLocation model identifier or URL.
     * @param poolingMode pooling strategy to apply.
     * @param engine engine name (e.g. PyTorch).
     * @return a loaded {@link ZooModel}.
     * @throws Exception when the model cannot be loaded.
     */
    public abstract ZooModel<String, float[]> loadModel(String modelLocation, String poolingMode, String engine)
            throws Exception;
}
