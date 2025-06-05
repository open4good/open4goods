package org.open4goods.services.imageprocessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.open4goods.model.resource.ImageInfo;

/**
 * Basic tests for {@link ImageMagickService} using the {@link SimpleImageAnalyser}
 * logic to read image dimensions.
 */
public class ImageMagickServiceTest {

    @Test
    public void testBuildImageInfo() throws Exception {
        File tmp = Files.createTempFile("img", ".png").toFile();
        BufferedImage img = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", tmp);

        ImageMagickService service = new ImageMagickService();
        ImageInfo info = service.buildImageInfo(tmp);
        assertNotNull(info);
        assertEquals(20, info.getHeight());
        assertEquals(10, info.getWidth());
    }
}
