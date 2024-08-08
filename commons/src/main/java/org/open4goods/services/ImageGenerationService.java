package org.open4goods.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.open4goods.config.yml.ui.ImageGenerationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;


/**
 * This service is in charge of generating images using OPENAI API and saving them to images folder
 *
 */
public class ImageGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ImageGenerationService.class);
    private final OpenAiImageModel imageModel;
    private final ImageGenerationConfig imageGenerationConfig;
    private String imagesFolder;

    public ImageGenerationService(OpenAiImageModel imageModel, ImageGenerationConfig imageGenerationConfig, String imagesFolder) {
        this.imageModel = imageModel;
        this.imageGenerationConfig = imageGenerationConfig;
        this.imagesFolder = imagesFolder;
    }

    public ImageResponse generateImage(String promptContent) {
        ImageOptions imageOptions = ImageOptionsBuilder.builder()
                .withN(1)
                .withHeight(1024)
                .withWidth(1024)
                .build();

        return imageModel.call(new ImagePrompt(promptContent, imageOptions));
    }

    public File saveImage(String imageUrl, String fileName) throws IOException {
        URL url = new URL(imageUrl);
        InputStream in = url.openStream();
        String outputDirectory = imagesFolder;
        File outputDir = new File(outputDirectory);

        if (!outputDir.exists()) {
            logger.warn("Output directory {} does not exist. Creating it.", outputDirectory);
            if (!outputDir.mkdirs()) {
                logger.error("Failed to create output directory {}", outputDirectory);
                throw new IOException("Failed to create directory " + outputDirectory);
            }
        }

        File outputFile = new File(outputDirectory, fileName);

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        } finally {
            in.close();
        }

        return outputFile;
    }

    public String generatePrompt(String vertical) {
        return imageGenerationConfig.getPrompt().replace("{VERTICAL}", vertical);
    }

    public String getImagesFolder() {
        return imagesFolder;
    }

    public boolean shouldGenerateImage(String fileName) {
        File file = new File(imagesFolder, fileName);
        return !file.exists() || imageGenerationConfig.isForceOverride();
    }

    public File fullGenerate(String verticalTitle, String fileName) throws IOException {
        String promptContent = generatePrompt(verticalTitle);
        String imageUrl = generateImage(promptContent).getResult().getOutput().getUrl();
        return saveImage(imageUrl, fileName);
    }
}
