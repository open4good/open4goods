package org.open4goods.api.services.migration.legacybackup;

import java.util.Map;
import java.util.Optional;

/**
 * Outcome of one {@code APPLY} invocation, possibly covering only part of the dataset when
 * cancelled or resumed.
 *
 * @param datasetId dataset imported
 * @param status terminal status of this invocation
 * @param outcomeCounts per-{@link LegacyBackupApplyOutcome} line counts accumulated in this
 *     invocation only
 * @param finalCursor durable checkpoint position after this invocation
 * @param failureReason sanitized reason, present only when {@code status} is {@link LegacyBackupApplyStatus#FAILED}
 */
public record LegacyBackupApplyResult(
        String datasetId,
        LegacyBackupApplyStatus status,
        Map<LegacyBackupApplyOutcome, Long> outcomeCounts,
        LegacyBackupCursor finalCursor,
        Optional<String> failureReason) {
}
