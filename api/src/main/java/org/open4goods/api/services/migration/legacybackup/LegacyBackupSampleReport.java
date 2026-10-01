package org.open4goods.api.services.migration.legacybackup;

import java.util.Map;

/**
 * Dry-run classification of a bounded sample of lines from the first manifest file: writes
 * nothing to any store, but exercises the same converter APPLY will use.
 *
 * @param inventory the underlying inventory this sample was taken against
 * @param sampledLines number of lines actually classified
 * @param convertibleCount lines that produced a source-record mutation
 * @param deadLetterCountsByReason dead letters that would be raised, grouped by reason
 */
public record LegacyBackupSampleReport(
        LegacyBackupInventoryReport inventory,
        long sampledLines,
        long convertibleCount,
        Map<LegacyBackupDeadLetterReason, Long> deadLetterCountsByReason) {
}
