package org.open4goods.model.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductSourceUrlsTest {

    @Test
    void add_ShouldDeduplicateByCanonicalUrlAndCapStoredUrls() {
        ProductSourceUrls sourceUrls = new ProductSourceUrls();
        sourceUrls.setMaxStoredUrls(2);

        ProductSourceUrl first = new ProductSourceUrl("https://www.example.com/product?utm_source=test");
        first.setTitle("First");
        first.setSerpRank(2);
        ProductSourceUrl duplicate = new ProductSourceUrl("https://example.com/product");
        duplicate.setSnippet("Merged snippet");
        duplicate.setSerpRank(1);
        ProductSourceUrl third = new ProductSourceUrl("https://other.example/product");
        third.setSerpRank(3);

        sourceUrls.add(first);
        sourceUrls.add(duplicate);
        sourceUrls.add(third);

        assertThat(sourceUrls.getUrls()).hasSize(2);
        assertThat(sourceUrls.getUrls().getFirst().getTitle()).isEqualTo("First");
        assertThat(sourceUrls.getUrls().getFirst().getSnippet()).isEqualTo("Merged snippet");
        assertThat(sourceUrls.getUrls()).extracting(ProductSourceUrl::getCanonicalUrl)
                .containsExactly("https://example.com/product", "https://other.example/product");
    }

    @Test
    void productSourceUrls_ShouldExposeFetchedSources() {
        Product product = new Product();
        ProductSourceUrl sourceUrl = new ProductSourceUrl("https://example.com/review");
        sourceUrl.setStatus(ProductSourceUrlStatus.FETCHED);
        sourceUrl.setMarkdown("markdown");
        sourceUrl.setTokenCount(42);
        sourceUrl.setFetchStrategy("HTTP");
        sourceUrl.setContentHash("hash");

        product.getSourceUrls().add(sourceUrl);

        assertThat(product.getSourceUrls().fetched()).singleElement()
                .satisfies(source -> {
                    assertThat(source.getUrl()).isEqualTo("https://example.com/review");
                    assertThat(source.getMarkdown()).isEqualTo("markdown");
                    assertThat(source.getTokenCount()).isEqualTo(42);
                });
    }
}
