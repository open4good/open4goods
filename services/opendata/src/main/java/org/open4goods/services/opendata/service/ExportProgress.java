package org.open4goods.services.opendata.service;

import java.time.Duration;
import java.util.Locale;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Progress of one Elasticsearch read of an open data pass (GOU-2556): at most one INFO line per {@code interval}, with
 * the products read, the total expected from the index count, the percentage, the rate and the elapsed time. The
 * {@code org.open4goods.services.opendata} logger is kept at INFO in api, above the WARN of {@code org.open4goods}.
 */
final class ExportProgress {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportProgress.class);

    private final String pass;
    private final long expected;
    private final long intervalNanos;
    private final LongSupplier nanoTime;
    private final long startNanos;
    private long lastLineNanos;

    /**
     * @param pass     name of the read, in the lines ({@code main}, {@code impact})
     * @param expected products counted in the index at the start of the pass; zero or less when unknown
     * @param interval shortest delay between two lines
     * @param nanoTime monotonic clock, in nanoseconds
     */
    ExportProgress(String pass, long expected, Duration interval, LongSupplier nanoTime) {
        this.pass = pass;
        this.expected = expected;
        this.intervalNanos = interval == null ? 0 : interval.toNanos();
        this.nanoTime = nanoTime;
        this.startNanos = nanoTime.getAsLong();
        this.lastLineNanos = startNanos;
    }

    ExportProgress(String pass, long expected, Duration interval) {
        this(pass, expected, interval, System::nanoTime);
    }

    /**
     * Logs a line when {@code interval} has elapsed since the previous one.
     *
     * @param read products read so far
     * @return the logged line, or {@code null} when it is too early
     */
    String update(long read) {
        long now = nanoTime.getAsLong();
        if (now - lastLineNanos < intervalNanos) {
            return null;
        }
        lastLineNanos = now;
        String line = line(read, now);
        LOGGER.info(line);
        return line;
    }

    /**
     * Logs the last line of the read, whatever the interval.
     *
     * @param read products read
     * @return the logged line
     */
    String finish(long read) {
        String line = line(read, nanoTime.getAsLong()) + ", done";
        LOGGER.info(line);
        return line;
    }

    private String line(long read, long now) {
        Duration elapsed = Duration.ofNanos(now - startNanos);
        double minutes = elapsed.toNanos() / 60_000_000_000d;
        long rate = minutes > 0 ? Math.round(read / minutes) : 0;
        StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "Open data %s pass: %d / %s products read",
                pass, read, expected > 0 ? String.valueOf(expected) : "?"));
        if (expected > 0) {
            line.append(String.format(Locale.ROOT, " (%.1f %%)", read * 100d / expected));
        }
        line.append(String.format(Locale.ROOT, ", %d products/min, %s elapsed", rate, format(elapsed)));
        if (expected > read && rate > 0) {
            line.append(", about ").append(format(Duration.ofMinutes(Math.ceilDiv(expected - read, rate))))
                    .append(" left");
        }
        return line.toString();
    }

    static String format(Duration duration) {
        return String.format(Locale.ROOT, "%dh%02dm%02ds", duration.toHours(), duration.toMinutesPart(),
                duration.toSecondsPart());
    }
}
