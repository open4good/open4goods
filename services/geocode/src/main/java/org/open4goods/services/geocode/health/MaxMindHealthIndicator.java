package org.open4goods.services.geocode.health;

import org.open4goods.services.geocode.service.IpGeolocationService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator reporting whether the MaxMind database is loaded.
 */
@Component
public class MaxMindHealthIndicator implements HealthIndicator
{
    private final IpGeolocationService ipGeolocationService;

    /**
     * Creates a new health indicator.
     *
     * @param ipGeolocationService IP geolocation service
     */
    public MaxMindHealthIndicator(IpGeolocationService ipGeolocationService)
    {
        this.ipGeolocationService = ipGeolocationService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Health health()
    {
        if (ipGeolocationService.isLoaded())
        {
            return Health.up().withDetail("databaseLoaded", true).build();
        }
        // TODO : Fix
        return Health.up().withDetail("databaseLoaded", false).build();
    }
}
