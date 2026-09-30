package org.open4goods.api.services.migration.legacybackup;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.pricehistory.service.LegacyPriceBackfillService;

/**
 * Applies one converted line to the target stores, using {@link SourceRecordHeadStore#find} as
 * the sole dedup oracle instead of an in-process map.
 *
 * <p>This keeps memory use independent of dataset cardinality (AC7) and, when the importer
 * processes each dataset's files strictly in manifest order with a single writer, makes the
 * "first occurrence wins, later conflicting occurrences are quarantined" outcome a function of
 * pinned input order rather than of thread arrival order (AC5).
 */
public final class LegacyBackupDedupCoordinator {

    private final SourceRecordHeadStore sourceRecordStore;
    private final LegacyPriceBackfillService priceBackfillService;
    private final LegacyBackupDeadLetterSink deadLetterSink;

    public LegacyBackupDedupCoordinator(
            SourceRecordHeadStore sourceRecordStore,
            LegacyPriceBackfillService priceBackfillService,
            LegacyBackupDeadLetterSink deadLetterSink) {
        this.sourceRecordStore = Objects.requireNonNull(sourceRecordStore, "sourceRecordStore must not be null");
        this.priceBackfillService = Objects.requireNonNull(priceBackfillService, "priceBackfillService must not be null");
        this.deadLetterSink = Objects.requireNonNull(deadLetterSink, "deadLetterSink must not be null");
    }

    /**
     * Applies one conversion outcome.
     *
     * @param datasetId dataset the line belongs to
     * @param fileName archive file the line came from
     * @param lineNumber 1-based line position within {@code fileName}
     * @param conversion conversion outcome for the line
     * @param recordedAt instant used for any newly raised dead letter
     * @return what happened to this line
     */
    public LegacyBackupApplyOutcome apply(
            String datasetId, String fileName, long lineNumber, LegacyBackupRecordConversion conversion, Instant recordedAt) {
        for (LegacyBackupDeadLetter informational : conversion.deadLetters()) {
            deadLetterSink.record(informational);
        }
        if (conversion.mutation().isEmpty()) {
            return LegacyBackupApplyOutcome.UNCONVERTIBLE;
        }

        SourceRecordMutation mutation = conversion.mutation().orElseThrow();
        SourceRecordHead candidate = mutation.candidate();
        Optional<SourceRecordHead> existing = sourceRecordStore.find(candidate.key());

        if (existing.isPresent() && existing.orElseThrow().payloadHash().equals(candidate.payloadHash())) {
            return LegacyBackupApplyOutcome.DUPLICATE_IDENTICAL_SKIPPED;
        }
        if (existing.isPresent()) {
            deadLetterSink.record(new LegacyBackupDeadLetter(
                    datasetId, fileName, lineNumber, LegacyBackupDeadLetterReason.CONFLICTING_DUPLICATE_GTIN,
                    "gtin=" + candidate.key().sourceRecordId().value()
                            + " existingHash=" + existing.orElseThrow().payloadHash().hexadecimalValue()
                            + " candidateHash=" + candidate.payloadHash().hexadecimalValue(),
                    recordedAt));
            return LegacyBackupApplyOutcome.CONFLICT_QUARANTINED;
        }

        try {
            sourceRecordStore.apply(mutation);
        } catch (RuntimeException exception) {
            deadLetterSink.record(new LegacyBackupDeadLetter(
                    datasetId, fileName, lineNumber, LegacyBackupDeadLetterReason.BULK_APPLY_FAILURE,
                    safeMessage(exception), recordedAt));
            throw exception;
        }
        conversion.price().ifPresent(priceBackfillService::importOnce);
        return LegacyBackupApplyOutcome.APPLIED;
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message;
    }
}
