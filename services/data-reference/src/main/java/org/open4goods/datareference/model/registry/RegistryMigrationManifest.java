package org.open4goods.datareference.model.registry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Complete, reviewable migration disposition for the legacy attribute corpus.
 *
 * <p>The manifest deliberately has no writer. It is generated from reviewed
 * Git definitions and can be checked against the legacy resource inventory by
 * build tooling; runtime consumers receive only its immutable coverage report.
 *
 * @param registryVersion registry revision that reviewed the dispositions
 * @param migrations one disposition for every legacy attribute resource
 */
public record RegistryMigrationManifest(RegistryVersion registryVersion, List<LegacyAttributeMigration> migrations) {

    /** Validates a duplicate-free, immutable set of legacy dispositions. */
    public RegistryMigrationManifest {
        Objects.requireNonNull(registryVersion, "registryVersion must not be null");
        migrations = List.copyOf(Objects.requireNonNull(migrations, "migrations must not be null"));
        Set<String> resources = new LinkedHashSet<>();
        for (LegacyAttributeMigration migration : migrations) {
            if (!resources.add(Objects.requireNonNull(migration, "migration must not be null").legacyResource())) {
                throw new RegistryValidationException("duplicate legacy attribute migration: "
                        + migration.legacyResource());
            }
        }
    }

    /**
     * Returns the complete legacy resource inventory declared by this manifest.
     *
     * @return immutable, duplicate-free legacy resource paths
     */
    public Set<String> legacyResources() {
        Set<String> resources = new LinkedHashSet<>();
        migrations.forEach(migration -> resources.add(migration.legacyResource()));
        return Set.copyOf(resources);
    }

    /**
     * Verifies that every reviewed replacement is addressable by the registry
     * and that no known legacy resource is omitted.
     *
     * @param registry immutable O4G registry that owns replacement ids
     * @param legacyResources complete current legacy attribute resource inventory
     * @return machine-readable coverage report for build evidence
     */
    public RegistryMigrationCoverageReport verify(InMemoryCanonicalRegistry registry, Set<String> legacyResources) {
        Objects.requireNonNull(registry, "registry must not be null");
        if (!registryVersion.equals(registry.version())) {
            throw new RegistryValidationException("migration manifest version " + registryVersion
                    + " does not match registry version " + registry.version());
        }
        legacyResources = Set.copyOf(Objects.requireNonNull(legacyResources, "legacyResources must not be null"));
        if (legacyResources.stream().anyMatch(resource -> resource == null || resource.isBlank())) {
            throw new IllegalArgumentException("legacyResources must not contain blank paths");
        }
        Set<String> accountedFor = new LinkedHashSet<>();
        int migrated = 0;
        int merged = 0;
        int retired = 0;
        for (LegacyAttributeMigration migration : migrations) {
            if (!legacyResources.contains(migration.legacyResource())) {
                throw new RegistryValidationException("migration references an unknown legacy attribute resource: "
                        + migration.legacyResource());
            }
            if (registry.findAttribute(migration.replacement()).isEmpty()) {
                throw new RegistryValidationException("migration replacement is not addressable: "
                        + migration.replacement());
            }
            accountedFor.add(migration.legacyResource());
            switch (migration.status()) {
                case MIGRATED -> migrated++;
                case MERGED -> merged++;
                case RETIRED -> retired++;
            }
        }
        if (!accountedFor.equals(legacyResources)) {
            Set<String> missing = new LinkedHashSet<>(legacyResources);
            missing.removeAll(accountedFor);
            throw new RegistryValidationException("legacy attribute resources missing from migration manifest: " + missing);
        }
        return new RegistryMigrationCoverageReport(registryVersion, legacyResources.size(), migrated, merged, retired);
    }
}
