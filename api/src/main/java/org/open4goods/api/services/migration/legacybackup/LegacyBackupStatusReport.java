package org.open4goods.api.services.migration.legacybackup;

/**
 * Current checkpoint progress for a dataset, read without touching its files.
 *
 * @param datasetId dataset reported on
 * @param hasCheckpoint {@code false} when no {@code APPLY} has ever committed progress
 * @param cursor current durable position; {@link LegacyBackupCursor#START} when {@code hasCheckpoint} is {@code false}
 * @param cancellationRequested whether a pending cancellation request is outstanding
 */
public record LegacyBackupStatusReport(
        String datasetId, boolean hasCheckpoint, LegacyBackupCursor cursor, boolean cancellationRequested) {
}
