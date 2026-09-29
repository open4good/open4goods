package org.open4goods.model.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies the public provider label registry stays deny-by-default and
 * never derives a label from an internal source id.
 */
class PublicProviderLabelRegistryTest {

    @Test
    void shippedRegistryHasNoRealLabelEntries() throws Exception {
        PublicProviderLabelRegistry registry = PublicProviderLabelRegistry.loadDefault();

        assertThat(registry.find("amazon.fr")).isEmpty();
        assertThat(registry.find("any-source")).isEmpty();
    }

    @Test
    void unmappedSourceIdYieldsNoLabel() {
        PublicProviderLabelRegistry registry = new PublicProviderLabelRegistry(new PublicProviderLabelDocument(List.of(
                new PublicProviderLabel("mapped-source.example", "Mapped Source", null))));

        assertThat(registry.find("unmapped-source.example")).isEmpty();
        assertThat(registry.find(null)).isEmpty();
    }

    @Test
    void mappedSourceIdReturnsItsReviewedLabel() {
        PublicProviderLabelRegistry registry = new PublicProviderLabelRegistry(new PublicProviderLabelDocument(List.of(
                new PublicProviderLabel("mapped-source.example", "Mapped Source", "https://mapped.example/favicon.ico"))));

        assertThat(registry.find("mapped-source.example")).contains(
                new PublicProviderLabel("mapped-source.example", "Mapped Source", "https://mapped.example/favicon.ico"));
    }

    @Test
    void rejectsDuplicateSourceIds() {
        PublicProviderLabelDocument duplicate = new PublicProviderLabelDocument(List.of(
                new PublicProviderLabel("dup.example", "First", null),
                new PublicProviderLabel("dup.example", "Second", null)));

        assertThatThrownBy(() -> new PublicProviderLabelRegistry(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dup.example");
    }

    @Test
    void loadsACheckedInTestFixtureSetDistinctFromTheShippedRegistry() throws Exception {
        PublicProviderLabelRegistry registry =
                PublicProviderLabelRegistry.load("/provider-labels/test-fixture-public-provider-labels.json");

        assertThat(registry.find("test-fixture-merchant-alpha.example"))
                .map(PublicProviderLabel::label)
                .contains("Test Fixture Merchant Alpha");
        assertThat(registry.find("test-fixture-merchant-unknown.example")).isEmpty();
    }
}
