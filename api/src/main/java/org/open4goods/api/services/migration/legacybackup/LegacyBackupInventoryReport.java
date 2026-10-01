package org.open4goods.api.services.migration.legacybackup;

import java.util.List;

/**
 * Read-only inventory of one pinned dataset: manifest identity, per-file digest/line-count
 * verification, and the registry/policy/mapping versions this importer will apply. Writes
 * nothing.
 *
 * @param datasetId dataset inventoried
 * @param schemaVersion manifest schema version
 * @param mappingVersion reviewed field-disposition mapping version
 * @param usagePolicyId usage policy id this importer stamps on every written head
 * @param usagePolicyVersion usage policy version this importer stamps on every written head
 * @param files per-file verification results, in manifest order
 * @param totalLines sum of verified line counts across every file
 */
public record LegacyBackupInventoryReport(
        String datasetId,
        String schemaVersion,
        String mappingVersion,
        String usagePolicyId,
        String usagePolicyVersion,
        List<VerifiedFile> files,
        long totalLines) {

    /**
     * @param name archive file name
     * @param bytes compressed size
     * @param lineCount verified decompressed line count
     */
    public record VerifiedFile(String name, long bytes, long lineCount) {
    }
}
