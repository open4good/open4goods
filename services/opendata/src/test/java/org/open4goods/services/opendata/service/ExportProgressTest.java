package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ExportProgressTest {

    private final AtomicLong now = new AtomicLong();

    @Test
    void progressShouldLogAtMostOneLinePerIntervalWithPercentageAndRate() {
        ExportProgress progress = new ExportProgress("main", 4000, Duration.ofMinutes(5), now::get);

        now.set(Duration.ofMinutes(1).toNanos());
        assertThat(progress.update(500)).isNull();

        now.set(Duration.ofMinutes(5).toNanos());
        assertThat(progress.update(1000)).isEqualTo(
                "Open data main pass: 1000 / 4000 products read (25.0 %), 200 products/min, 0h05m00s elapsed,"
                        + " about 0h15m00s left");

        now.set(Duration.ofMinutes(9).toNanos());
        assertThat(progress.update(1800)).isNull();

        now.set(Duration.ofMinutes(20).toNanos());
        assertThat(progress.finish(4000)).isEqualTo(
                "Open data main pass: 4000 / 4000 products read (100.0 %), 200 products/min, 0h20m00s elapsed, done");
    }

    @Test
    void unknownTotalShouldGiveNoPercentage() {
        ExportProgress progress = new ExportProgress("impact", 0, Duration.ZERO, now::get);

        now.set(Duration.ofSeconds(30).toNanos());

        assertThat(progress.update(100)).isEqualTo(
                "Open data impact pass: 100 / ? products read, 200 products/min, 0h00m30s elapsed");
    }

    @Test
    void progressLinesShouldUseTheOpenDataLoggerKeptAtInfoInApi() {
        // api keeps org.open4goods.services.opendata at INFO, above the WARN of org.open4goods in beta and prod.
        assertThat(LoggerFactory.getLogger(ExportProgress.class).getName()).startsWith("org.open4goods.services.opendata.");
    }
}
