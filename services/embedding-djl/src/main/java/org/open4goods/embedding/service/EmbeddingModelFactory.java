package org.open4goods.embedding.service;

import ai.djl.repository.zoo.ZooModel;

/**
 * Factory responsible for loading DJL text embedding models.
 */
public interface EmbeddingModelFactory
{
    /**
     * Load a DJL model for the provided location.
     *
     * @param modelLocation model identifier or local path.
     * @param poolingMode pooling strategy to apply.
     * @param normalize whether the translator should normalise output vectors.
     * @param engine engine name (e.g. PyTorch)
     * @return a loaded {@link ZooModel}
     * @throws Exception when the model cannot be loaded
     */
    ZooModel<String, float[]> loadModel(String modelLocation, String poolingMode, boolean normalize, String engine) throws Exception;
}
