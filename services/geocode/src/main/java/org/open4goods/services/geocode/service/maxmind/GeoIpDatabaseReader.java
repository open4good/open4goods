package org.open4goods.services.geocode.service.maxmind;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

/**
 * Abstraction over the MaxMind GeoIP database reader.
 */
public interface GeoIpDatabaseReader extends Closeable
{
    /**
     * Resolves a CityResponse for the given IP address.
     *
     * @param address IP address
     * @return city response
     * @throws IOException when IO fails
     * @throws GeoIp2Exception when lookup fails
     */
    CityResponse city(InetAddress address) throws IOException, GeoIp2Exception;
}
