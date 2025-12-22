package org.open4goods.api.services.completion.text;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.open4goods.api.config.yml.ApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.djl.Application;
import ai.djl.huggingface.translator.TextEmbeddingTranslator;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;

@Service
public class DjlTextEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(DjlTextEmbeddingService.class);

    @Autowired
    private ApiProperties apiProperties;

    private ZooModel<String, float[]> textModel;
    private ZooModel<String, float[]> multimodalTextModel;

    @PostConstruct
    public void init() {
        this.textModel = loadModel(apiProperties.getEmbedding().getTextModelUrl(), "text-only");
        this.multimodalTextModel = loadModel(apiProperties.getEmbedding().getMultimodalModelUrl(), "multimodal");
    }

    @PreDestroy
    public void close() {
        closeQuietly(textModel);
        closeQuietly(multimodalTextModel);
    }

    /**
     * Computes the embedding for a single text.
     * @param text
     * @return normalized embedding vector
     */
    public float[] embed(String text) {
        float[] vector = embedWithModel(textModel, text);
        if (vector != null) {
            return vector;
        }
        return embedWithModel(multimodalTextModel, text);
    }
    
    // Simple batch wrapper (real batching would require BatchPredictor)
    public List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).collect(Collectors.toList());
    }

    private ZooModel<String, float[]> loadModel(String modelUrl, String label) {
        try {
            logger.info("Initializing DJL {} embedding model from: {}", label, modelUrl);

            HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(modelUrl);

            TextEmbeddingTranslator translator = TextEmbeddingTranslator.builder(tokenizer)
                    .optPoolingMode("mean")
                    .optNormalize(true)
                    .build();

            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optApplication(Application.NLP.TEXT_EMBEDDING)
                    .optModelUrls(modelUrl)
                    .optTranslator(translator)
                    .optEngine("PyTorch")
                    .optProgress(new ProgressBar())
                    .build();

            return criteria.loadModel();
        } catch (Exception e) {
            logger.error("Failed to initialize {} text embedding model", label, e);
            return null;
        }
    }

    private float[] embedWithModel(ZooModel<String, float[]> model, String text) {
        if (model == null || text == null) {
            return null;
        }
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            return predictor.predict(text);
        } catch (Exception e) {
            logger.error("Error embedding text: {}", text, e);
            return null;
        }
    }

    private void closeQuietly(ZooModel<String, float[]> model) {
        if (model != null) {
            model.close();
        }
    }
}
