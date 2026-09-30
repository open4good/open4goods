package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.registry.GitRegistryRuntimeImporter;

class IcecatCategoryVerticalResolverTest {

    private final IcecatCategoryVerticalResolver resolver = new IcecatCategoryVerticalResolver(
            new IcecatRegistryProjectionService(new GitRegistryRuntimeImporter()));

    @Test
    void resolvesTheVerticalOfAReviewedIcecatCategoryMapping() {
        // icecat category:1584 maps to o4g:class:television, included in the "tv" vertical view.
        assertThat(resolver.resolveVerticalId(1584, LocalDate.of(2026, 9, 12))).hasValue("tv");
    }

    @Test
    void resolvesEmptyWhenNoIcecatCategoryIdIsGiven() {
        assertThat(resolver.resolveVerticalId(null, LocalDate.of(2026, 9, 12))).isEmpty();
    }

    @Test
    void resolvesEmptyWhenTheCategoryHasNoReviewedMapping() {
        assertThat(resolver.resolveVerticalId(999_999, LocalDate.of(2026, 9, 12))).isEmpty();
    }

    @Test
    void resolvesEmptyWhenTheMappingIsNotYetEffective() {
        assertThat(resolver.resolveVerticalId(1584, LocalDate.of(2020, 1, 1))).isEmpty();
    }
}
