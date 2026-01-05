package org.open4goods.commons.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/**
 * Unit tests covering the time-window quota logic handled by {@link IpQuotaService}.
 */
class IpQuotaServiceTest {

    @Test
    void windowedQuotaTracksUsageWithinWindow() {
        Clock fixedClock = Clock.fixed(Instant.parse("2024-02-02T10:15:30Z"), ZoneOffset.UTC);
        IpQuotaService service = new IpQuotaService(fixedClock);

        Duration window = Duration.ofMinutes(30);
        String action = "REVIEW_GENERATION";
        String ip = "127.0.0.1";

        assertThat(service.getUsage(action, ip, window)).isZero();
        assertThat(service.isAllowed(action, ip, 2, window)).isTrue();

        service.increment(action, ip, window);
        service.increment(action, ip, window);

        assertThat(service.getUsage(action, ip, window)).isEqualTo(2);
        assertThat(service.getRemaining(action, ip, 2, window)).isZero();
        assertThat(service.isAllowed(action, ip, 2, window)).isFalse();
    }

    @Test
    void windowedQuotaRejectsInvalidWindow() {
        Clock fixedClock = Clock.fixed(Instant.parse("2024-02-02T10:15:30Z"), ZoneOffset.UTC);
        IpQuotaService service = new IpQuotaService(fixedClock);

        assertThatThrownBy(() -> service.increment("ACTION", "127.0.0.1", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quota window must be greater than zero");
    }
}
