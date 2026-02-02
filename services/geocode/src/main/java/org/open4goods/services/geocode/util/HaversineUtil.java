package org.open4goods.services.geocode.util;

/**
 * Utility methods for great-circle distance calculations.
 */
public final class HaversineUtil
{
    /**
     * Mean Earth radius in kilometers (WGS-84 mean radius).
     */
    public static final double EARTH_RADIUS_KM = 6371.0088d;

    private HaversineUtil()
    {
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
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2)
    {
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double sinLat = Math.sin(deltaLat / 2d);
        double sinLon = Math.sin(deltaLon / 2d);

        double a = sinLat * sinLat
                + Math.cos(latRad1) * Math.cos(latRad2) * sinLon * sinLon;
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Computes the distance in meters between two coordinates.
     *
     * @param lat1 latitude of the first point
     * @param lon1 longitude of the first point
     * @param lat2 latitude of the second point
     * @param lon2 longitude of the second point
     * @return distance in meters
     */
    public static long distanceMeters(double lat1, double lon1, double lat2, double lon2)
    {
        return Math.round(distanceKm(lat1, lon1, lat2, lon2) * 1000d);
    }
}
