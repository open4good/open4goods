package org.open4goods.services.geocode.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HaversineUtil}.
 */
class HaversineUtilTest
{
    @Test
    void distanceMatchesExpectedRange()
    {
        double parisLat = 48.8566;
        double parisLon = 2.3522;
        double nycLat = 40.7143;
        double nycLon = -74.0060;

        double distanceKm = HaversineUtil.distanceKm(parisLat, parisLon, nycLat, nycLon);
        assertThat(distanceKm).isBetween(5800d, 5900d);
    }
}
