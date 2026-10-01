package org.open4goods.api.services.migration.legacybackup;

import java.time.Instant;
import java.util.Objects;

/**
 * One sanitized dead-letter record: a coordinate, a stable reason code, and detail free of raw
 * legacy field values beyond the GTIN and hash coordinates already needed to review a conflict.
 *
 * @param datasetId dataset this line belongs to
 * @param fileName archive file the line came from
 * @param lineNumber 1-based line position within {@code fileName}
 * @param reason stable, sanitized reason code
 * @param detail short, sanitized human-readable detail
 * @param recordedAt instant this dead letter was written
 */
public record LegacyBackupDeadLetter(
        String datasetId, String fileName, long lineNumber, LegacyBackupDeadLetterReason reason, String detail,
        Instant recordedAt) {

    /** Validates a bounded, sanitized dead letter. */
    public LegacyBackupDeadLetter {
        Objects.requireNonNull(datasetId, "datasetId must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (detail != null && detail.length() > 1024) {
            detail = detail.substring(0, 1024);
        }
    }
}
