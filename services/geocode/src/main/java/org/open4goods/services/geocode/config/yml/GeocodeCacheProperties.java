package org.open4goods.services.geocode.config.yml;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for local cache storage used by the geocode service.
 */
@ConfigurationProperties(prefix = "geocode.cache")
public class GeocodeCacheProperties
{
    private String path = "target/geocode-cache";

    /**
     * Returns the filesystem path used for cached remote files.
     *
     * @return cache path
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Sets the filesystem path used for cached remote files.
     *
     * @param path cache path
     */
    public void setPath(String path)
    {
        this.path = path;
    }
}
