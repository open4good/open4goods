package org.open4goods.nudgerfrontapi.service.geoip;

import java.net.InetAddress;

import org.open4goods.nudgerfrontapi.dto.user.UserGeolocDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;

/**
 * Resolves an IP address to a {@link UserGeolocDto} using the local MaxMind
 * GeoLite2 City database. City lookup only (latitude/longitude, country, city).
 * Returns {@code null} when GeoIP is disabled, the database is unavailable, or
 * the address cannot be resolved — callers then fall back to the geocode service.
 */
@Service
public class GeoIpResolutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoIpResolutionService.class);

    private final GeoIpDatabaseManager databaseManager;

    public GeoIpResolutionService(GeoIpDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * @return true when the local database is loaded and ready
     */
    public boolean isAvailable() {
        return databaseManager.available();
    }

    /**
     * @param ip the client IP address
     * @return the geolocation, or {@code null} when it cannot be resolved locally
     */
    public UserGeolocDto resolve(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        DatabaseReader reader = databaseManager.reader().orElse(null);
        if (reader == null) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(ip.trim());
            CityResponse city = reader.city(address);
            if (city == null) {
                return null;
            }
            Location location = city.location();
            Subdivision subdivision = city.subdivisions().isEmpty() ? null : city.subdivisions().getFirst();
            Integer accuracy = location == null ? null : location.getAccuracyRadius();
            return new UserGeolocDto(
                    ip,
                    city.continent().name(),
                    city.continent().code(),
                    city.country().name(),
                    city.country().isoCode(),
                    city.registeredCountry().name(),
                    city.registeredCountry().isoCode(),
                    city.city().name(),
                    subdivision == null ? null : subdivision.name(),
                    subdivision == null ? null : subdivision.isoCode(),
                    city.postal().code(),
                    location == null ? null : location.latitude(),
                    location == null ? null : location.longitude(),
                    accuracy,
                    location == null ? null : location.timeZone(),
                    null, // metroCode removed in MaxMind 5.0.0
                    false, // isAnonymousProxy removed in MaxMind 5.0.0
                    city.traits().isAnycast());
        } catch (Exception e) {
            LOGGER.debug("Local GeoIP resolution failed for {}: {}", ip, e.getMessage());
            return null;
        }
    }
}
