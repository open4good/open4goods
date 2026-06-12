package org.open4goods.nudgerfrontapi.utils;

/**
 * Great-circle distance helper. Copied from the {@code services/geocode}
 * microservice (which is a standalone Spring Boot app, not a shared library) so
 * front-api can compute user-to-factory distances in-process.
 */
public final class HaversineUtil {

    /** Mean Earth radius in kilometers (WGS-84 mean radius). */
    public static final double EARTH_RADIUS_KM = 6371.0088d;

    private HaversineUtil() {
    }

    /**
     * Computes the distance in kilometers between two coordinates.
     *
     * @param lat1 latitude of the first point
     * @param lon1 longitude of the first point
     * @param lat2 latitude of the second point
     * @param lon2 longitude of the second point
     * @return distance in kilometers
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double sinLat = Math.sin(deltaLat / 2d);
        double sinLon = Math.sin(deltaLon / 2d);

        double a = sinLat * sinLat + Math.cos(latRad1) * Math.cos(latRad2) * sinLon * sinLon;
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return EARTH_RADIUS_KM * c;
    }
}
