package org.open4goods.datareference.model.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class GitRegistryRuntimeImporterTest {

    private final GitRegistryRuntimeImporter importer = new GitRegistryRuntimeImporter(new GitRegistryLoader(),
            Clock.fixed(Instant.parse("2026-09-11T07:30:00Z"), ZoneOffset.UTC));

    @Test
    void publishesTheCompleteValidatedRegistryAndMakesExactRetriesIdempotent() throws IOException {
        RegistryImportReport installed = importer.importDefault();
        RegistryImportReport retried = importer.importDefault();

        assertThat(installed.alias()).isEqualTo(GitRegistryRuntimeImporter.READ_ALIAS);
        assertThat(installed.idempotent()).isFalse();
        assertThat(installed.installedAt()).isEqualTo(Instant.parse("2026-09-11T07:30:00Z"));
        assertThat(installed.classCount()).isEqualTo(7);
        assertThat(installed.attributeCount()).isEqualTo(2);
        assertThat(retried.idempotent()).isTrue();
        assertThat(retried.contentHash()).isEqualTo(installed.contentHash());
        assertThat(importer.current()).hasValueSatisfying(index -> assertThat(index.contentHash())
                .isEqualTo(installed.contentHash()));
    }

    @Test
    void rejectsChangedContentUntilItsRegistryVersionAdvances() throws IOException {
        importer.importDefault();

        assertThatThrownBy(() -> importer.importGitResource(bytes(registryJson().replace("\"Width\"", "\"Product width\""))))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("without a version increment");

        RegistryImportReport upgraded = importer.importGitResource(bytes(registryJson()
                .replace("\"registryVersion\": 1", "\"registryVersion\": 2")
                .replace("\"Width\"", "\"Product width\"")));
        assertThat(upgraded.registryVersion()).isEqualTo(new RegistryVersion(2));
        assertThat(upgraded.idempotent()).isFalse();
    }

    private static ByteArrayInputStream bytes(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String registryJson() {
        try (var stream = GitRegistryRuntimeImporterTest.class.getResourceAsStream(GitRegistryLoader.REGISTRY_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("registry test resource is missing");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read registry test resource", exception);
        }
    }
}
