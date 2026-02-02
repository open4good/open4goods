package org.open4goods.services.geocode.config;

import org.open4goods.services.geocode.config.yml.GeocodeCacheProperties;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for geocode service infrastructure beans.
 */
@Configuration
public class GeocodeConfiguration
{
    /**
     * Creates a RemoteFileCachingService for dataset downloads.
     *
     * @param cacheProperties cache properties
     * @param remoteFileCachingProperties remote caching configuration
     * @return remote file caching service
     */
    @Bean
    public RemoteFileCachingService remoteFileCachingService(GeocodeCacheProperties cacheProperties,
            RemoteFileCachingProperties remoteFileCachingProperties)
    {
        return new RemoteFileCachingService(cacheProperties.getPath(), remoteFileCachingProperties);
    }
}
