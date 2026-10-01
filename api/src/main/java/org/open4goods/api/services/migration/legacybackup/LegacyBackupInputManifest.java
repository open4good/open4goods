package org.open4goods.api.services.migration.legacybackup;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable input contract produced by {@code scripts/migration/pin_product_backup.py}.
 *
 * <p>This importer only reads this manifest; it never regenerates or edits it. Unknown
 * properties (sample, scope, ...) are ignored rather than rejected so a richer manifest
 * remains usable.
 *
 * @param schemaVersion manifest schema version, must equal {@link #SCHEMA_VERSION}
 * @param legacyManifest legacy publisher manifest this pin was taken against
 * @param files per-file digest, size and line count, in the order files must be imported
 * @param conversionRules reviewed conversion map applied by this importer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LegacyBackupInputManifest(
        String schemaVersion,
        LegacyManifestSection legacyManifest,
        List<LegacyBackupFileEntry> files,
        ConversionRules conversionRules) {

    /** Only schema version this importer understands. */
    public static final String SCHEMA_VERSION = "open4goods.product-backup-input/v1";

    /** Validates presence of the fields this importer depends on. */
    public LegacyBackupInputManifest {
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(legacyManifest, "legacyManifest must not be null");
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("manifest must list at least one file");
        }
        files = List.copyOf(files);
        Objects.requireNonNull(conversionRules, "conversionRules must not be null");
    }

    /**
     * @param sha256 digest of the legacy publisher's own manifest bytes
     * @param completedAt ISO-8601 completion instant of the legacy export
     * @param completedEpochMillis completion instant in epoch milliseconds
     * @param expectedCount expected exported record count
     * @param exportedCount actual exported record count
     * @param files ordered gzip file names the legacy publisher listed
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegacyManifestSection(
            String sha256,
            String completedAt,
            long completedEpochMillis,
            long expectedCount,
            long exportedCount,
            List<String> files) {

        /** Validates required legacy-manifest fields. */
        public LegacyManifestSection {
            Objects.requireNonNull(sha256, "sha256 must not be null");
            Objects.requireNonNull(completedAt, "completedAt must not be null");
            if (files == null || files.isEmpty()) {
                throw new IllegalArgumentException("legacyManifest.files must not be empty");
            }
            files = List.copyOf(files);
        }
    }

    /**
     * @param name gzip file name, must match {@code products-backup-<n>.gz}
     * @param sha256 digest of the file's compressed bytes
     * @param bytes compressed file size
     * @param lineCount decoded JSONL line count
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegacyBackupFileEntry(String name, String sha256, long bytes, long lineCount) {

        /** Validates a safe, bounded file entry. */
        public LegacyBackupFileEntry {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(sha256, "sha256 must not be null");
            if (lineCount < 0) {
                throw new IllegalArgumentException("lineCount must not be negative");
            }
        }
    }

    /**
     * @param mappingVersion reviewed field-disposition mapping version this manifest was built against
     * @param attributeDispositionManifest repository-relative path to the 94-entry attribute registry
     * @param fieldDispositions per observed field name, its reviewed conversion disposition
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConversionRules(
            String mappingVersion,
            String attributeDispositionManifest,
            Map<String, FieldDispositionEntry> fieldDispositions) {

        /** Only mapping version this importer understands. */
        public static final String MAPPING_VERSION = "legacy-product-fields/v1";

        /** Validates presence of the reviewed mapping version. */
        public ConversionRules {
            Objects.requireNonNull(mappingVersion, "mappingVersion must not be null");
            fieldDispositions = fieldDispositions == null ? Map.of() : Map.copyOf(fieldDispositions);
        }
    }

    /**
     * @param classification reviewed disposition classification for one observed field name
     * @param rule human-readable rationale recorded by the pin tool
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FieldDispositionEntry(String classification, String rule) {
    }
}
