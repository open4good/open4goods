package org.open4goods.api.services.completion.image;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
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

    /**
     * Path to the directory or file where the model is stored.
     * Example: file:/opt/models/vision-embedder/
     *
     * Adjust this to your environment.
     */
    // TODO : From conf
    private static final String MODEL_URL = "file:/opt/open4goods/classification/restnet/model.onnx";

    /**
     * Optional model name inside the directory (depends on how you exported it).
     * If your model is a single ONNX file, you may not need this.
     */
    private static final String MODEL_NAME = "model"; // adapt to your case

    /** Target image size expected by the model (e.g. 224x224). */
    private static final int IMAGE_SIZE = 224;

    private ZooModel<Image, float[]> model;
    private Predictor<Image, float[]> predictor;

    /**
     * Initializes the DJL model and predictor once at startup.
     * @throws ModelNotFoundException
     */
    @PostConstruct
    public void init() throws IOException, MalformedModelException, ModelNotFoundException {
        logger.info("Initializing DJL image embedding model from: {}", MODEL_URL);

        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optModelUrls(MODEL_URL)
                .optModelName(MODEL_NAME)            // can be omitted if not needed
                .optTranslator(new EmbeddingTranslator(IMAGE_SIZE))
                .optProgress(new ProgressBar())
                .build();

        model = criteria.loadModel();
        predictor = model.newPredictor();

        logger.info("DJL image embedding model initialized successfully.");
    }

    /**
     * Releases model resources on shutdown.
     */
    @PreDestroy
    public void close() {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }

    @Override
    public float[] embed(Path imagePath) throws Exception {
        try {
            Image img = ImageFactory.getInstance().fromFile(imagePath);
            return predictor.predict(img);
        } catch (TranslateException e) {
            logger.error("Error while computing embedding for {}: {}", imagePath, e.getMessage());
            throw e;
        }
    }
}
