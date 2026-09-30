package org.open4goods.api.services.migration.legacybackup;

import java.time.Instant;
import java.time.LocalDate;

import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicy;

/**
 * Versioned deny-all policy for records imported from the legacy product backup.
 *
 * <p>This importer preserves GTIN identity and a small set of native evidenced fields without
 * inventing provenance or redistribution rights (ADR-0014): the original legacy Product data's
 * publication rights were never reviewed, so nothing imported here is eligible for any
 * projection surface until an owner explicitly reviews and republishes this policy.
 */
public final class LegacyBackupUsagePolicy {

    /** Stable policy source coordinate for legacy-backup-derived source records. */
    public static final SourceId SOURCE_ID = new SourceId("legacy-backup");

    /** Immutable deny-all policy used until an owner reviews legacy-backup publication rights. */
    public static final SourceUsagePolicy POLICY = SourceUsagePolicy.denyAll(
            "legacy-backup-import", SOURCE_ID, "1", Instant.parse("2026-09-30T00:00:00Z"),
            LocalDate.of(2026, 9, 30));

    private LegacyBackupUsagePolicy() {
    }
}
