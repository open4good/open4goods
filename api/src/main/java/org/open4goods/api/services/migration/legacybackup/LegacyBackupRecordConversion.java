package org.open4goods.api.services.migration.legacybackup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.pricehistory.model.LegacyMinimumPricePoint;

/**
 * Outcome of converting one legacy backup JSONL line.
 *
 * <p>Exactly one of {@link #mutation()} or {@link #deadLetters()} being non-trivial: a
 * convertible line yields a mutation (with an optional price point and informational, non-fatal
 * dead letters for deferred fields); an unconvertible line yields only dead letters.
 *
 * @param mutation source-record replacement, present when the line carried a valid GTIN
 * @param price legacy minimum price point, when the line carried a convertible price field
 * @param deadLetters dead letters raised while converting this line, informational or blocking
 */
public record LegacyBackupRecordConversion(
        Optional<SourceRecordMutation> mutation,
        Optional<LegacyMinimumPricePoint> price,
        List<LegacyBackupDeadLetter> deadLetters) {

    /** Defensively copies the dead-letter list. */
    public LegacyBackupRecordConversion {
        Objects.requireNonNull(mutation, "mutation must not be null");
        Objects.requireNonNull(price, "price must not be null");
        deadLetters = List.copyOf(Objects.requireNonNull(deadLetters, "deadLetters must not be null"));
    }
}
