package org.open4goods.embeddinggateway.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.open4goods.embeddinggateway.config.EmbeddingGatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * DJL-backed implementation that fetches images from URLs and exposes normalised embeddings.
 */
@Service
public class DjlImageEmbeddingService implements ImageEmbeddingService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DjlImageEmbeddingService.class);

    private final EmbeddingGatewayProperties properties;
    private ZooModel<Image, float[]> model;

    public DjlImageEmbeddingService(EmbeddingGatewayProperties properties)
    {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws ModelNotFoundException, MalformedModelException, java.io.IOException
    {
        Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optModelUrls(properties.getImageModelUrl())
                .optTranslator(new ImageEmbeddingTranslator(properties.getImageInputSize()))
                .optProgress(new ProgressBar())
                .build();

        model = criteria.loadModel();
        LOGGER.info("Image embedding model loaded from {} (inputSize={})", properties.getImageModelUrl(),
                properties.getImageInputSize());
    }

    @Override
    public float[] embed(URL imageUrl) throws Exception
    {
        if (model == null)
        {
            throw new IllegalStateException("Image embedding model is not initialised");
        }

        Path tempFile = Files.createTempFile("embedding-gateway-image", ".bin");
        try
        {
            download(imageUrl, tempFile);
            Image image = ImageFactory.getInstance().fromFile(tempFile);

            try (Predictor<Image, float[]> predictor = model.newPredictor())
            {
                return predictor.predict(image);
            }
            catch (TranslateException e)
            {
                LOGGER.error("Unable to embed image from {}: {}", imageUrl, e.getMessage());
                throw e;
            }
        }
        finally
        {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public boolean isReady()
    {
        return model != null;
    }

    @PreDestroy
    public void close()
    {
        if (model != null)
        {
            model.close();
        }
    }

    private void download(URL imageUrl, Path destination) throws Exception
    {
        HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
        connection.setConnectTimeout((int) properties.getImageDownloadTimeout().toMillis());
        connection.setReadTimeout((int) properties.getImageDownloadTimeout().toMillis());
        connection.setInstanceFollowRedirects(true);

        connection.connect();
        int status = connection.getResponseCode();
        if (status >= 400)
        {
            throw new IllegalStateException("Image download failed with HTTP status " + status);
        }

        try (InputStream input = connection.getInputStream();
                var output = Files.newOutputStream(destination))
        {
            StreamUtils.copy(input, output);
        }
    }
}
