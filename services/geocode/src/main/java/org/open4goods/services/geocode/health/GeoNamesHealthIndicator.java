package org.open4goods.services.geocode.health;

import org.open4goods.services.geocode.service.GeoNamesIndexService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator reporting whether the GeoNames index is loaded.
 */
@Component
public class GeoNamesHealthIndicator implements HealthIndicator
{
    private final GeoNamesIndexService indexService;

    /**
     * Creates a new health indicator.
     *
     * @param indexService index service
     */
    public GeoNamesHealthIndicator(GeoNamesIndexService indexService)
    {
        this.indexService = indexService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Health health()
    {
        if (indexService.isLoaded())
        {
            return Health.up().withDetail("indexLoaded", true).build();
        }
        return Health.down().withDetail("indexLoaded", false).build();
    }
}
