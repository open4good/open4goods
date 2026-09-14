package org.open4goods.datareference.model.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.serialization.DataReferenceJson;

class RegistryMigrationManifestTest {

    private final InMemoryCanonicalRegistry registry = loadRegistry();

    @Test
    void producesCompleteMachineReadableCoverageForReviewedLegacyMigrations() throws IOException {
        RegistryMigrationManifest manifest = new RegistryMigrationManifest(new RegistryVersion(4), List.of(
                migration("WIDTH.yml", LegacyAttributeMigrationStatus.MIGRATED, "width"),
                migration("COLOR.yml", LegacyAttributeMigrationStatus.MERGED, "color")));

        RegistryMigrationCoverageReport report = manifest.verify(registry, Set.of(
                "verticals/src/main/resources/attributes/WIDTH.yml",
                "verticals/src/main/resources/attributes/COLOR.yml"));

        assertThat(report.registryVersion()).isEqualTo(new RegistryVersion(4));
        assertThat(report.legacyAttributeCount()).isEqualTo(2);
        assertThat(report.migratedCount()).isEqualTo(1);
        assertThat(report.mergedCount()).isEqualTo(1);
        assertThat(report.retiredCount()).isZero();
        assertThat(DataReferenceJson.mapper().writeValueAsString(report)).isEqualTo(
                "{\"registryVersion\":4,\"legacyAttributeCount\":2,\"migratedCount\":1,\"mergedCount\":1,\"retiredCount\":0}");
    }

    @Test
    void refusesASilentLegacyAttributeDropAndAnUnaddressableReplacement() {
        RegistryMigrationManifest incomplete = new RegistryMigrationManifest(new RegistryVersion(4), List.of(
                migration("WIDTH.yml", LegacyAttributeMigrationStatus.MIGRATED, "width")));
        RegistryMigrationManifest dangling = new RegistryMigrationManifest(new RegistryVersion(4), List.of(
                migration("WIDTH.yml", LegacyAttributeMigrationStatus.MIGRATED, "missing")));
        Set<String> inventory = Set.of("verticals/src/main/resources/attributes/WIDTH.yml",
                "verticals/src/main/resources/attributes/COLOR.yml");

        assertThatThrownBy(() -> incomplete.verify(registry, inventory))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("missing from migration manifest");
        assertThatThrownBy(() -> dangling.verify(registry, Set.of("verticals/src/main/resources/attributes/WIDTH.yml")))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("replacement is not addressable");
    }

    @Test
    void refusesDuplicateOrUnsafeLegacyResourceCoordinates() {
        assertThatThrownBy(() -> new RegistryMigrationManifest(new RegistryVersion(3), List.of(
                migration("WIDTH.yml", LegacyAttributeMigrationStatus.MIGRATED, "width"),
                migration("WIDTH.yml", LegacyAttributeMigrationStatus.MERGED, "width"))))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("duplicate legacy attribute migration");
        assertThatThrownBy(() -> new LegacyAttributeMigration("../WIDTH.yml", LegacyAttributeMigrationStatus.MIGRATED,
                new CanonicalAttributeId("width"), "path traversal must not be accepted"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legacyResource");
    }

    @Test
    void loadsAndAccountsForEveryCurrentLegacyAttributeConfiguration() throws IOException {
        GitRegistryLoader loader = new GitRegistryLoader();
        RegistryMigrationManifest manifest = loader.loadDefaultMigrationManifest();

        RegistryMigrationCoverageReport report = manifest.verify(registry, currentLegacyResources());

        assertThat(report.registryVersion()).isEqualTo(registry.version());
        assertThat(report.legacyAttributeCount()).isEqualTo(94);
        assertThat(report.migratedCount()).isEqualTo(93);
        assertThat(report.mergedCount()).isEqualTo(1);
        assertThat(report.retiredCount()).isZero();
        assertThat(manifest.migrations()).anySatisfy(migration -> {
            assertThat(migration.legacyResource()).endsWith("COULEUR_EXTERIEURE.yml");
            assertThat(migration.status()).isEqualTo(LegacyAttributeMigrationStatus.MERGED);
            assertThat(migration.replacement()).isEqualTo(new CanonicalAttributeId("color"));
        });
    }

    private static LegacyAttributeMigration migration(String filename, LegacyAttributeMigrationStatus status,
            String replacement) {
        return new LegacyAttributeMigration("verticals/src/main/resources/attributes/" + filename, status,
                new CanonicalAttributeId(replacement), "Reviewed during canonical registry migration.");
    }

    private static InMemoryCanonicalRegistry loadRegistry() {
        try {
            return new GitRegistryLoader().loadDefault().registry();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load checked-in registry", exception);
        }
    }

    private static Set<String> currentLegacyResources() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve(".git"))) {
            root = root.getParent();
            if (root == null) {
                throw new IllegalStateException("cannot locate repository root from the Maven working directory");
            }
        }
        Path repositoryRoot = root;
        Path attributes = repositoryRoot.resolve("verticals/src/main/resources/attributes");
        try (var resources = Files.list(attributes)) {
            return resources.filter(resource -> resource.getFileName().toString().endsWith(".yml"))
                    .map(resource -> repositoryRoot.relativize(resource).toString().replace('\\', '/'))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
