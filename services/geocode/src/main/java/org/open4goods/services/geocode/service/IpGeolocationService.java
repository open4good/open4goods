package org.open4goods.services.geocode.service;

import org.open4goods.services.geocode.dto.IpGeolocationResponse;

/**
 * Service API for IP geolocation.
 */
public interface IpGeolocationService
{
    /**
     * Resolves an IP address to MaxMind GeoIP information.
     *
     * @param ip IP address
     * @return geolocation response, or null when not found
     */
    IpGeolocationResponse resolve(String ip);

    /**
     * Returns whether the GeoIP database has been loaded.
     *
     * @return true when loaded
     */
    boolean isLoaded();
}
