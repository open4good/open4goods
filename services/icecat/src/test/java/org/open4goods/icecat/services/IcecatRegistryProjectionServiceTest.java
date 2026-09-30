package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.registry.GitRegistryRuntimeImporter;

class IcecatRegistryProjectionServiceTest {

    @Test
    void rejectsAStaleHashBeforeReplacingTheCompleteProjection() {
        IcecatRegistryProjectionService service = new IcecatRegistryProjectionService(new GitRegistryRuntimeImporter());
        String installedHash = service.current().contentHash();

        assertThatThrownBy(() -> service.rebuild("0".repeat(64)))
                .isInstanceOf(StaleRegistryProjectionException.class)
                .hasMessageContaining("does not match installed hash");

        assertThat(service.current().contentHash()).isEqualTo(installedHash);
    }

    @Test
    void acceptsTheCurrentHashAndRetainsAnExactGitRevisionIdempotently() {
        IcecatRegistryProjectionService service = new IcecatRegistryProjectionService(new GitRegistryRuntimeImporter());
        String installedHash = service.current().contentHash();

        assertThat(service.rebuild(installedHash).idempotent()).isTrue();
        assertThat(service.current().contentHash()).isEqualTo(installedHash);
    }
}
