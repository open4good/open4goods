package org.open4goods.services.urlfetching.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.services.urlfetching.dto.ExtractedMetadataAttribute;
import org.open4goods.services.urlfetching.dto.ResourceType;

/**
 * Unit tests for official brand resource extraction.
 */
class OfficialResourceExtractorTest {

    @Test
    void extract_AddsPdfVideoAndStructuredImagesForOfficialBrandHost() {
        String html = """
                <html>
                  <head><meta property="og:image" content="/media/product.webp"></head>
                  <body>
                    <a href="/docs/manual.pdf">Manual</a>
                    <video><source src="/videos/demo.mp4"></video>
                    <iframe src="https://www.youtube.com/embed/abc123" title="Demo"></iframe>
                    <img src="/layout/logo.png">
                  </body>
                </html>
                """;
        List<ExtractedMetadataAttribute> metadata = List.of(
                new ExtractedMetadataAttribute("brand", "Acme", "jsonld", null),
                new ExtractedMetadataAttribute("og:image", "/media/product.webp", "meta", null));

        var resources = OfficialResourceExtractor.extract("https://shop.acme.com/products/1", html, metadata);

        assertThat(resources).extracting("type")
                .containsExactly(ResourceType.PDF, ResourceType.VIDEO, ResourceType.VIDEO, ResourceType.IMAGE);
        assertThat(resources).extracting("url")
                .contains("https://shop.acme.com/docs/manual.pdf",
                        "https://shop.acme.com/videos/demo.mp4",
                        "https://www.youtube.com/embed/abc123",
                        "https://shop.acme.com/media/product.webp")
                .doesNotContain("https://shop.acme.com/layout/logo.png");
    }

    @Test
    void extract_SkipsResourcesWhenHostDoesNotContainBrand() {
        String html = "<a href=\"/docs/manual.pdf\">Manual</a>";
        List<ExtractedMetadataAttribute> metadata = List.of(
                new ExtractedMetadataAttribute("brand", "Acme", "jsonld", null));

        var resources = OfficialResourceExtractor.extract("https://merchant.example/products/1", html, metadata);

        assertThat(resources).isEmpty();
    }
}
