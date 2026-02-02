package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.config.properties.GeocodeProperties;
import org.open4goods.nudgerfrontapi.dto.user.UserGeolocDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Service implementation that delegates user geolocation to the geocode microservice.
 */
@Service
public class UserGeolocationServiceImpl implements UserGeolocationService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserGeolocationServiceImpl.class);

    private final RestClient restClient;

    /**
     * Creates a new geolocation service.
     *
     * @param restClientBuilder REST client builder
     * @param geocodeProperties geocode configuration properties
     */
    public UserGeolocationServiceImpl(RestClient.Builder restClientBuilder, GeocodeProperties geocodeProperties)
    {
        this.restClient = restClientBuilder.baseUrl(geocodeProperties.getBaseUrl()).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserGeolocDto resolve(String ip)
    {
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
