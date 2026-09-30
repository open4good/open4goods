package org.open4goods.api.services.migration.legacybackup;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

import org.open4goods.datareference.model.SourceId;

/**
 * Explicit, fully-named request for one legacy backup import operation.
 *
 * <p>Every field is mandatory: there is no default dataset, no default manifest and no implicit
 * "latest" resolution (AC1). {@code datasetId} follows the same bounded pattern
 * {@link SourceId} enforces, since it is stored as one.
 *
 * @param datasetId immutable, bounded identifier for this pinned dataset/import run
 * @param manifestPath absolute path to the pinned, read-only input manifest produced by
 *     {@code scripts/migration/pin_product_backup.py}
 */
public record LegacyBackupImportRequest(String datasetId, Path manifestPath) {

    private static final Pattern DATASET_ID_PATTERN = Pattern.compile("[a-z0-9]+(?:[.\\-][a-z0-9]+)*");

    /** Validates a bounded dataset id and an absolute manifest path. */
    public LegacyBackupImportRequest {
        Objects.requireNonNull(datasetId, "datasetId must not be null");
        if (!DATASET_ID_PATTERN.matcher(datasetId).matches()) {
            throw new IllegalArgumentException(
                    "datasetId must be lower-case alphanumeric separated by '.' or '-': " + datasetId);
        }
        Objects.requireNonNull(manifestPath, "manifestPath must not be null");
        if (!manifestPath.isAbsolute()) {
            throw new IllegalArgumentException("manifestPath must be absolute");
        }
    }
}
