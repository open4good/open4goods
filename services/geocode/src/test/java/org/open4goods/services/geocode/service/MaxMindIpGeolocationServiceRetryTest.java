package org.open4goods.services.geocode.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.open4goods.services.geocode.service.maxmind.GeoIpDatabaseReader;
import org.open4goods.services.geocode.service.maxmind.GeoIpDatabaseReaderFactory;
import org.open4goods.services.geocode.service.maxmind.MaxMindDatasetProvider;

/**
 * Covers the recovery of the GeoIP database after a failed first download.
 */
class MaxMindIpGeolocationServiceRetryTest
{
    private static final Duration RETRY = Duration.ofMinutes(5);

    /** Mutable clock for deterministic throttling checks. */
    private static final class MutableClock extends Clock
    {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d)
        {
            now = now.plus(d);
        }

        @Override
        public java.time.ZoneId getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return now;
        }
    }

    @Test
    void recoversAfterFailedStartupDownloadWithoutHammeringTheSource() throws Exception
    {
        MaxMindDatasetProvider provider = mock(MaxMindDatasetProvider.class);
        GeoIpDatabaseReaderFactory factory = mock(GeoIpDatabaseReaderFactory.class);
        Path db = Path.of("db.mmdb");
        when(provider.getDatabasePath())
                .thenThrow(new IllegalStateException("download failed"))
                .thenReturn(db);
        when(factory.create(any())).thenReturn(mock(GeoIpDatabaseReader.class));
        MutableClock clock = new MutableClock();
        MaxMindIpGeolocationService service = new MaxMindIpGeolocationService(provider, factory, clock, RETRY);

        service.initialize();
        assertFalse(service.isLoaded());

        // Within the backoff window: no new download attempt.
        assertThrows(IllegalStateException.class, () -> service.resolve("1.2.3.4"));
        service.retryLoadIfNeeded();
        verify(provider, times(1)).getDatabasePath();
        assertFalse(service.isLoaded());

        // After the backoff: the scheduled retry loads the database.
        clock.advance(RETRY.plusSeconds(1));
        service.retryLoadIfNeeded();
        verify(provider, times(2)).getDatabasePath();
        assertTrue(service.isLoaded());
    }
}
