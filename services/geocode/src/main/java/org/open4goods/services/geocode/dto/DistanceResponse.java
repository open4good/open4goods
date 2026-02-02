package org.open4goods.services.geocode.dto;

/**
 * Response payload for distance computations.
 *
 * @param from origin geocode response
 * @param to destination geocode response
 * @param distanceKm distance in kilometers
 * @param distanceMeters distance in meters
 */
public record DistanceResponse(
        GeocodeResponse from,
        GeocodeResponse to,
        double distanceKm,
        long distanceMeters)
{
}
