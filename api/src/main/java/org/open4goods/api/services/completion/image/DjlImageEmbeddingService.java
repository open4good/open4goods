package org.open4goods.api.services.completion.image;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import org.open4goods.api.config.yml.ApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.Path;

/**
 * {@link ImageEmbeddingService} implementation backed by DJL (Deep Java Library).
 *
 * <p>
 * Loads a pre-trained vision model (e.g. ResNet / ViT exported to ONNX)
 * and exposes a simple float[] embedding for any image.
 * </p>
 *
 * <p>
 * This class is category-agnostic: it does not predict labels, only
 * returns a dense feature vector that can be used for:
 * <ul>
 *     <li>Per-product grouping / clustering</li>
 *     <li>Similarity / duplicate detection</li>
 *     <li>Outlier / mismatch detection</li>
 * </ul>
 * </p>
 */
@Service
@org.springframework.context.annotation.Profile("!local")
public class DjlImageEmbeddingService implements ImageEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(DjlImageEmbeddingService.class);

    private final ApiProperties apiProperties;
    private ZooModel<Image, float[]> model;

    public DjlImageEmbeddingService(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    /**
     * Initializes the DJL model once at startup.
     * The model is thread-safe and can be shared, but predictors are created per-request.
     * @throws ModelNotFoundException
     */
    @PostConstruct
    public void init() throws IOException, MalformedModelException, ModelNotFoundException {
        final String modelUrl = apiProperties.getEmbedding().getMultimodalModelUrl();
        final int imageSize = apiProperties.getEmbedding().getImageInputSize();

        logger.info("Initializing DJL image embedding model from: {}", modelUrl);

        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optModelUrls(modelUrl)
                .optTranslator(new EmbeddingTranslator(imageSize))
                .optProgress(new ProgressBar())
                .build();

        model = criteria.loadModel();

        logger.info("DJL image embedding model initialized successfully.");
    }

    /**
     * Releases model resources on shutdown.
     */
    @PreDestroy
    public void close() {
        if (model != null) {
            model.close();
        }
    }

    @Override
    public float[] embed(Path imagePath) throws Exception {
        // Create a new predictor for each request to ensure thread safety.
        // Predictors are lightweight and manage their own NDManager lifecycle.
        try (Predictor<Image, float[]> predictor = model.newPredictor()) {
            Image img = ImageFactory.getInstance().fromFile(imagePath);
            return predictor.predict(img);
        } catch (TranslateException e) {
            logger.error("Error while computing embedding for {}: {}", imagePath, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error while computing embedding for {}: {}", imagePath, e.getMessage(), e);
            throw e;
        }
    }
}
