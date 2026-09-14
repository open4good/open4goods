package org.open4goods.datareference.model.registry;

import java.util.Objects;

/**
 * Machine-readable reconciliation evidence for one registry migration manifest.
 *
 * @param registryVersion O4G registry version that resolved every replacement
 * @param legacyAttributeCount current number of legacy attribute resources
 * @param migratedCount one-to-one migrations
 * @param mergedCount deliberate merges into a shared O4G attribute
 * @param retiredCount deliberate retirements with a replacement
 */
public record RegistryMigrationCoverageReport(
        RegistryVersion registryVersion,
        int legacyAttributeCount,
        int migratedCount,
        int mergedCount,
        int retiredCount) {

    /** Validates complete, non-negative migration accounting. */
    public RegistryMigrationCoverageReport {
        Objects.requireNonNull(registryVersion, "registryVersion must not be null");
        if (legacyAttributeCount < 0 || migratedCount < 0 || mergedCount < 0 || retiredCount < 0) {
            throw new IllegalArgumentException("migration coverage counts must not be negative");
        }
        if (legacyAttributeCount != migratedCount + mergedCount + retiredCount) {
            throw new IllegalArgumentException("migration coverage counts must account for every legacy attribute");
        }
    }
}
