package org.open4goods.services.urlfetching.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for structured metadata extraction.
 */
class StructuredMetadataExtractorTest {

    @Test
    void extract_ReadsJsonLdMetaAndItemProps() {
        String html = """
                <html>
                  <head>
                    <meta property="og:title" content="OLED TV">
                    <script type="application/ld+json">
                      {
                        "@context": "https://schema.org",
                        "@type": "Product",
                        "name": "OLED TV",
                        "brand": {"@type": "Brand", "name": "Acme"},
                        "gtin13": "1234567890123",
                        "color": "Black"
                      }
                    </script>
                  </head>
                  <body>
                    <span itemprop="mpn" content="XR-42"></span>
                  </body>
                </html>
                """;

        StructuredMetadataExtractor.ExtractionResult result = StructuredMetadataExtractor.extract(html);

        assertThat(result.gtins()).containsExactly("1234567890123");
        assertThat(result.attributes())
                .anySatisfy(attribute -> {
                    assertThat(attribute.name()).isEqualTo("brand");
                    assertThat(attribute.value()).isEqualTo("Acme");
                    assertThat(attribute.source()).isEqualTo("jsonld");
                })
                .anySatisfy(attribute -> {
                    assertThat(attribute.name()).isEqualTo("og:title");
                    assertThat(attribute.value()).isEqualTo("OLED TV");
                    assertThat(attribute.source()).isEqualTo("meta");
                })
                .anySatisfy(attribute -> {
                    assertThat(attribute.name()).isEqualTo("mpn");
                    assertThat(attribute.value()).isEqualTo("XR-42");
                    assertThat(attribute.source()).isEqualTo("itemprop");
                });
    }
}
