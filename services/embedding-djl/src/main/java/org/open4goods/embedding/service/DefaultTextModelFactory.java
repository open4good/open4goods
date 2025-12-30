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

        String tokenizerName = resolveTokenizerName(modelLocation);
        HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(tokenizerName);
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

    private String resolveTokenizerName(String modelLocation)
    {
        if (modelLocation.startsWith("file:"))
        {
            try
            {
                return java.nio.file.Path.of(java.net.URI.create(modelLocation)).toAbsolutePath().toString();
            }
            catch (Exception e)
            {
                LOGGER.warn("Failed to parse file URI {}, falling back to original string", modelLocation);
                return modelLocation;
            }
        }
        else if (modelLocation.startsWith("djl://"))
        {
            // Format is usually djl://ai.djl.huggingface.pytorch/MODEL_ID
            // or djl://ai.djl.huggingface.pytorch/GROUP_ID/ARTIFACT_ID
            // HuggingFaceTokenizer needs "GROUP_ID/ARTIFACT_ID"
            
            // Remove the scheme
            String withoutScheme = modelLocation.substring("djl://".length());
            
            // Find the first slash which separates the DJL repository key (e.g. ai.djl.huggingface.pytorch) from the rest
            int firstSlash = withoutScheme.indexOf('/');
            if (firstSlash > 0 && firstSlash < withoutScheme.length() - 1)
            {
                return withoutScheme.substring(firstSlash + 1);
            }
        }
        return modelLocation;
    }
}
