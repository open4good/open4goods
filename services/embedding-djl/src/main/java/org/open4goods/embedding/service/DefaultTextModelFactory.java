package org.open4goods.embedding.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.djl.Application;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.translator.TextEmbeddingTranslator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;

/**
 * Default implementation that loads DJL models using HuggingFace tokenizers and translators.
 */
public class DefaultTextModelFactory implements EmbeddingModelFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTextModelFactory.class);

    @Override
    public ZooModel<String, float[]> loadModel(String modelLocation, String poolingMode, boolean normalize, String engine) throws Exception
    {
        LOGGER.info("Loading DJL embedding model from {} with pooling {}", modelLocation, poolingMode);

        HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(modelLocation);
        TextEmbeddingTranslator translator = TextEmbeddingTranslator.builder(tokenizer)
                .optPoolingMode(poolingMode)
                .optNormalize(normalize)
                .build();

        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .optModelUrls(modelLocation)
                .optTranslator(translator)
                .optEngine(engine)
                .optProgress(new ProgressBar())
                .build();

        return criteria.loadModel();
    }
}
