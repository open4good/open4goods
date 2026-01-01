package org.open4goods.embedding.service.image;

import java.io.IOException;

import ai.djl.MalformedModelException;
import ai.djl.modality.cv.Image;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;

/**
 * Default implementation that loads image embedding models with the DJL criteria API.
 */
public class DefaultImageModelFactory extends AbstractImageModelFactory
{
    @Override
    public ZooModel<Image, float[]> loadModel(String modelUrl, int imageSize)
            throws IOException, MalformedModelException, ModelNotFoundException
    {
        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optModelUrls(modelUrl)
                .optTranslator(new EmbeddingTranslator(imageSize))
                .optProgress(new ProgressBar())
                .build();

        return criteria.loadModel();
    }
}
