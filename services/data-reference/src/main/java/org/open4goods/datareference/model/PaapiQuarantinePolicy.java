package org.open4goods.datareference.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Versioned deny-all policy for historical Amazon PA-API evidence.
 *
 * <p>The only permitted Amazon treatment is independently evidenced ASIN
 * identity; PA-API records themselves are not eligible for any projection.
 */
public final class PaapiQuarantinePolicy {

    /** Stable policy source coordinate for explicitly identified PA-API evidence. */
    public static final SourceId SOURCE_ID = new SourceId("amazon-paapi");
    /** Immutable deny-all policy used by the reference importer and projections. */
    public static final SourceUsagePolicy POLICY = SourceUsagePolicy.denyAll(
            "amazon-paapi-quarantine", SOURCE_ID, "1", Instant.parse("2026-09-12T00:00:00Z"),
            LocalDate.of(2026, 9, 12));

    private PaapiQuarantinePolicy() {
    }
}
