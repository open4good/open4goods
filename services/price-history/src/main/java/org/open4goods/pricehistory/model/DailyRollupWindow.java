package org.open4goods.pricehistory.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Defines rollup mutability: current and previous UTC dates remain open for late observations.
 * Earlier dates require an explicit rebuild job.
 */
public final class DailyRollupWindow {

    private final Clock clock;

    /** Creates the standard two-day UTC open window. */
    public DailyRollupWindow(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /** Returns whether ordinary ingestion may update the date's bucket. */
    public boolean isOpen(LocalDate day) {
        Objects.requireNonNull(day, "day must not be null");
        return !day.isBefore(LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(1));
    }
}
