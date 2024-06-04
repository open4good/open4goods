package org.open4goods.services;

import org.open4goods.config.yml.ui.ImageGenerationConfig;
import org.springframework.ai.image.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


/**
 * This service is in charge of generating images using OPENAI API and saving them to images folder
 *
 */
public class ImageGenerationService {

    private final ImageClient imageClient;
    private final ImageGenerationConfig imageGenerationConfig;
    private String imagesFolder;

    public ImageGenerationService(ImageClient imageClient, ImageGenerationConfig imageGenerationConfig, String imagesFolder) {
        this.imageClient = imageClient;
        this.imageGenerationConfig = imageGenerationConfig;
        this.imagesFolder = imagesFolder;
    }

    public ImageResponse generateImage(String promptContent) {
        ImageOptions imageOptions = ImageOptionsBuilder.builder()
                .withN(1)
                .withHeight(1024)
                .withWidth(1024)
                .build();

        return imageClient.call(new ImagePrompt(promptContent, imageOptions));
    }

    public File saveImage(String imageUrl, String fileName) throws IOException {
        URL url = new URL(imageUrl);
        InputStream in = url.openStream();
        String outputDirectory = imagesFolder;
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
}
