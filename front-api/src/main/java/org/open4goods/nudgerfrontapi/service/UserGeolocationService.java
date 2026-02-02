package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.user.UserGeolocDto;

/**
 * Service API for retrieving user geolocation data.
 */
public interface UserGeolocationService
{
    /**
     * Resolves geolocation data for the given IP address.
     *
     * @param ip IP address
     * @return geolocation response, or null when the IP cannot be resolved
     */
    UserGeolocDto resolve(String ip);
}
