package org.open4goods.services.geocode.controller;

import org.open4goods.services.geocode.dto.DistanceResponse;
import org.open4goods.services.geocode.dto.GeocodeResponse;
import org.open4goods.services.geocode.dto.IpGeolocationResponse;
import org.open4goods.services.geocode.model.CityMatch;
import org.open4goods.services.geocode.service.GeocodeService;
import org.open4goods.services.geocode.service.IpGeolocationService;
import org.open4goods.services.geocode.util.HaversineUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for geocode lookups and distance calculations.
 */
@RestController
@RequestMapping("/v1")
public class GeocodeController
{
    private final GeocodeService geocodeService;
    private final IpGeolocationService ipGeolocationService;

    /**
     * Creates a new geocode controller.
     *
     * @param geocodeService geocode service
     */
    public GeocodeController(GeocodeService geocodeService, IpGeolocationService ipGeolocationService)
    {
        this.geocodeService = geocodeService;
        this.ipGeolocationService = ipGeolocationService;
    }

    /**
     * Resolves a city and country code to a GeoNames location.
     *
     * @param city city name
     * @param country country ISO-2 code
     * @return geocode response
     */
    @GetMapping("/geocode")
    public GeocodeResponse geocode(@RequestParam("city") String city, @RequestParam("country") String country)
    {
        validateRequired(city, "city");
        validateRequired(country, "country");
        CityMatch match = geocodeService.resolve(city, country);
        if (match == null)
        {
            throw new GeocodeNotFoundException("City not found for " + city + ", " + country);
        }
        return toResponse(city, country, match);
    }

    /**
     * Resolves IP geolocation data using the MaxMind database.
     *
     * @param ip IP address to resolve
     * @return geolocation response
     */
    @GetMapping("/geoloc")
    public IpGeolocationResponse geoloc(@RequestParam("ip") String ip)
    {
        validateRequired(ip, "ip");
        IpGeolocationResponse response = ipGeolocationService.resolve(ip);
        if (response == null)
        {
            throw new GeocodeNotFoundException("IP address not found for " + ip);
        }
        return response;
    }

    /**
     * Computes the distance between two resolved cities.
     *
     * @param fromCity origin city
     * @param fromCountry origin country
     * @param toCity destination city
     * @param toCountry destination country
     * @return distance response
     */
    @GetMapping("/distance")
    public DistanceResponse distance(@RequestParam("fromCity") String fromCity, @RequestParam("fromCountry") String fromCountry, @RequestParam("toCity") String toCity, @RequestParam("toCountry") String toCountry)
    {
        validateRequired(fromCity, "fromCity");
        validateRequired(fromCountry, "fromCountry");
        validateRequired(toCity, "toCity");
        validateRequired(toCountry, "toCountry");

        CityMatch fromMatch = geocodeService.resolve(fromCity, fromCountry);
        if (fromMatch == null)
        {
            throw new GeocodeNotFoundException("City not found for " + fromCity + ", " + fromCountry);
        }
        CityMatch toMatch = geocodeService.resolve(toCity, toCountry);
        if (toMatch == null)
        {
            throw new GeocodeNotFoundException("City not found for " + toCity + ", " + toCountry);
        }

        double distanceKm = HaversineUtil.distanceKm(
                fromMatch.record().latitude(),
                fromMatch.record().longitude(),
                toMatch.record().latitude(),
                toMatch.record().longitude());
        long distanceMeters = HaversineUtil.distanceMeters(
                fromMatch.record().latitude(),
                fromMatch.record().longitude(),
                toMatch.record().latitude(),
                toMatch.record().longitude());

        return new DistanceResponse(
                toResponse(fromCity, fromCountry, fromMatch),
                toResponse(toCity, toCountry, toMatch),
                distanceKm,
                distanceMeters);
    }

    private void validateRequired(String value, String field)
    {
        if (!StringUtils.hasText(value))
        {
            throw new GeocodeBadRequestException("Missing required parameter: " + field);
        }
    }

    private GeocodeResponse toResponse(String requestedCity, String requestedCountry, CityMatch match)
    {
        return new GeocodeResponse(
                requestedCity,
                requestedCountry,
                match.record().name(),
                match.record().geonameId(),
                match.record().latitude(),
                match.record().longitude(),
                match.record().population(),
                match.matchType());
    }
}
