package org.open4goods.services.imageprocessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.ImageGenerationConfig;

/**
 * Unit tests for {@link ImageGenerationService} focusing on utility methods that
 * don't require OpenAI connectivity.
 */
public class ImageGenerationServiceTest {

    @Test
    public void testGeneratePrompt() {
        ImageGenerationConfig cfg = new ImageGenerationConfig();
        cfg.setPrompt("hello {VERTICAL}");
        ImageGenerationService service = new ImageGenerationService(null, cfg, "target");
        assertEquals("hello tv", service.generatePrompt("tv"));
    }

    @Test
    public void testShouldGenerateImage() throws Exception {
        ImageGenerationConfig cfg = new ImageGenerationConfig();
        Path dir = Files.createTempDirectory("img");
        ImageGenerationService service = new ImageGenerationService(null, cfg, dir.toString());

        assertTrue(service.shouldGenerateImage("a.png"));

        Path file = dir.resolve("a.png");
        Files.createFile(file);
        cfg.setForceOverride(false);
        assertFalse(service.shouldGenerateImage("a.png"));
        cfg.setForceOverride(true);
        assertTrue(service.shouldGenerateImage("a.png"));
    }
}
