package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.config.properties.GeocodeProperties;
import org.open4goods.nudgerfrontapi.dto.user.UserGeolocDto;
import org.open4goods.nudgerfrontapi.service.geoip.GeoIpResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Resolves user geolocation, preferring the in-process MaxMind City database
 * when available and falling back to the geocode microservice otherwise.
 */
@Service
public class UserGeolocationServiceImpl implements UserGeolocationService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserGeolocationServiceImpl.class);

    private final RestClient restClient;
    private final GeoIpResolutionService geoIpResolutionService;

    /**
     * Creates a new geolocation service.
     *
     * @param restClientBuilder REST client builder
     * @param geocodeProperties geocode configuration properties
     * @param geoIpResolutionService local in-process GeoIP resolver
     */
    public UserGeolocationServiceImpl(RestClient.Builder restClientBuilder, GeocodeProperties geocodeProperties,
            GeoIpResolutionService geoIpResolutionService)
    {
        this.restClient = restClientBuilder.baseUrl(geocodeProperties.getBaseUrl()).build();
        this.geoIpResolutionService = geoIpResolutionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserGeolocDto resolve(String ip)
    {
        UserGeolocDto local = geoIpResolutionService.resolve(ip);
        if (local != null)
        {
            return local;
        }

        try
        {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v1/geoloc")
                            .queryParam("ip", ip)
                            .build())
                    .retrieve()
                    .body(UserGeolocDto.class);
        }
        catch (RestClientResponseException ex)
        {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND)
            {
                return null;
            }
            LOGGER.warn("Unexpected geocode response status {} for user geolocation", ex.getStatusCode());
            throw new IllegalStateException("Failed to retrieve user geolocation from geocode service", ex);
        }
        catch (RestClientException ex)
        {
            LOGGER.warn("Unable to call geocode service for user geolocation: {}", ex.getMessage());
            throw new IllegalStateException("Failed to call geocode service", ex);
        }
    }
}
