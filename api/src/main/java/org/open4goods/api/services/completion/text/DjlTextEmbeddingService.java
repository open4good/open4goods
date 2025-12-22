package org.open4goods.api.services.completion.text;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ai.djl.Model;
import ai.djl.huggingface.translator.TextEmbeddingTranslator;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;

@Service
public class DjlTextEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(DjlTextEmbeddingService.class);

    // DistilCamemBERT model (HuggingFace)
    // We can use the DJL model zoo to auto-download or point to a local path
    // "djl://ai.djl.huggingface.pytorch/cmarkea/distilcamembert-base"
    // For now, let's assume we use a criteria that fetches it.
    private static final String MODEL_FILTER = "cmarkea/distilcamembert-base";

    private ZooModel<String, float[]> model;

    @PostConstruct
    public void init() {
        try {
            logger.info("Initializing DJL text embedding model: {}", MODEL_FILTER);

            // Create tokenizer explicitly
            HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(MODEL_FILTER);

            TextEmbeddingTranslator translator = TextEmbeddingTranslator.builder(tokenizer)
                    .optPoolingMode("mean") // Mean pooling for sentence embedding
                    .optNormalize(true)
                    .build();

            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelName(MODEL_FILTER)
                    .optTranslator(translator)
                    .optEngine("PyTorch") // or OnnxRuntime
                    .optProgress(new ProgressBar())
                    .build();

            this.model = criteria.loadModel();
            
            logger.info("DJL text embedding model initialized.");
        } catch (Exception e) {
            logger.error("Failed to initialize text embedding model", e);
            // We don't throw to avoid crashing the app, but service won't work
        }
    }

    @PreDestroy
    public void close() {
        if (model != null) {
            model.close();
        }
    }

    /**
     * Computes the embedding for a single text.
     * @param text
     * @return float[768] vector
     */
    public float[] embed(String text) {
        if (model == null) {
            return null;
        }
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            return predictor.predict(text);
        } catch (Exception e) {
            logger.error("Error embedding text: {}", text, e);
            return null;
        }
    }
    
    // Simple batch wrapper (real batching would require BatchPredictor)
    public List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).collect(Collectors.toList());
    }
}
