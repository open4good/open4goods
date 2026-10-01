package org.open4goods.api.services.migration.legacybackup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupInputManifest.ConversionRules;
import org.open4goods.pricehistory.service.LegacyPriceBackfillService;

class LegacyBackupDedupCoordinatorTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final ConversionRules RULES = new ConversionRules(ConversionRules.MAPPING_VERSION, null, Map.of());

    private final LegacyBackupRecordConverter converter = new LegacyBackupRecordConverter(RULES);
    private InMemorySourceRecordHeadStore sourceRecordStore;
    private InMemoryLegacyPriceBackfillStore priceStore;
    private List<LegacyBackupDeadLetter> deadLetters;
    private LegacyBackupDedupCoordinator dedup;

    @BeforeEach
    void setUp() {
        sourceRecordStore = new InMemorySourceRecordHeadStore();
        priceStore = new InMemoryLegacyPriceBackfillStore();
        deadLetters = new ArrayList<>();
        dedup = new LegacyBackupDedupCoordinator(sourceRecordStore, new LegacyPriceBackfillService(priceStore),
                deadLetters::add);
    }

    @Test
    void firstOccurrenceIsApplied() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\",\"price\":9.99}", OBSERVED_AT);

        LegacyBackupApplyOutcome outcome = dedup.apply("dataset", "products-backup-0.gz", 1, conversion, OBSERVED_AT);

        assertThat(outcome).isEqualTo(LegacyBackupApplyOutcome.APPLIED);
        assertThat(sourceRecordStore.size()).isEqualTo(1);
        assertThat(priceStore.size()).isEqualTo(1);
    }

    @Test
    void identicalDuplicateIsSkippedWithoutRewritingOrDuplicatingPrice() {
        LegacyBackupRecordConversion first = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\",\"price\":9.99}", OBSERVED_AT);
        LegacyBackupRecordConversion duplicate = converter.convert("dataset", "products-backup-0.gz", 500,
                "{\"gtin\":\"4006381333931\",\"price\":9.99}", OBSERVED_AT);

        dedup.apply("dataset", "products-backup-0.gz", 1, first, OBSERVED_AT);
        LegacyBackupApplyOutcome outcome = dedup.apply("dataset", "products-backup-0.gz", 500, duplicate, OBSERVED_AT);

        assertThat(outcome).isEqualTo(LegacyBackupApplyOutcome.DUPLICATE_IDENTICAL_SKIPPED);
        assertThat(sourceRecordStore.size()).isEqualTo(1);
        assertThat(priceStore.size()).isEqualTo(1);
        assertThat(deadLetters).isEmpty();
    }

    @Test
    void conflictingDuplicateIsQuarantinedAndNeitherRecordWins() {
        LegacyBackupRecordConversion first = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"gtin\":\"4006381333931\",\"brand\":\"Acme\"}", OBSERVED_AT);
        LegacyBackupRecordConversion conflicting = converter.convert("dataset", "products-backup-1.gz", 42,
                "{\"gtin\":\"4006381333931\",\"brand\":\"OtherBrand\"}", OBSERVED_AT);

        dedup.apply("dataset", "products-backup-0.gz", 1, first, OBSERVED_AT);
        LegacyBackupApplyOutcome outcome = dedup.apply("dataset", "products-backup-1.gz", 42, conflicting, OBSERVED_AT);

        assertThat(outcome).isEqualTo(LegacyBackupApplyOutcome.CONFLICT_QUARANTINED);
        // The store keeps the first occurrence untouched; the conflicting one was never written.
        assertThat(sourceRecordStore.size()).isEqualTo(1);
        assertThat(deadLetters).extracting(LegacyBackupDeadLetter::reason)
                .contains(LegacyBackupDeadLetterReason.CONFLICTING_DUPLICATE_GTIN);
    }

    @Test
    void unconvertibleLineIsReportedButNotApplied() {
        LegacyBackupRecordConversion conversion = converter.convert("dataset", "products-backup-0.gz", 1,
                "{\"name\":\"no gtin\"}", OBSERVED_AT);

        LegacyBackupApplyOutcome outcome = dedup.apply("dataset", "products-backup-0.gz", 1, conversion, OBSERVED_AT);

        assertThat(outcome).isEqualTo(LegacyBackupApplyOutcome.UNCONVERTIBLE);
        assertThat(sourceRecordStore.size()).isZero();
        assertThat(deadLetters).extracting(LegacyBackupDeadLetter::reason)
                .containsExactly(LegacyBackupDeadLetterReason.MISSING_OR_INVALID_GTIN);
    }
}
