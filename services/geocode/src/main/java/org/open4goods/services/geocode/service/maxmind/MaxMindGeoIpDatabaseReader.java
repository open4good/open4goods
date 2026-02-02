package org.open4goods.services.geocode.service.maxmind;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

/**
 * MaxMind database reader backed by the official GeoIP2 {@link DatabaseReader}.
 */
public class MaxMindGeoIpDatabaseReader implements GeoIpDatabaseReader
{
    private final DatabaseReader databaseReader;

    /**
     * Creates a new reader using the given database path.
     *
     * @param databasePath path to the MaxMind database
     * @throws IOException when the database cannot be opened
     */
    public MaxMindGeoIpDatabaseReader(Path databasePath) throws IOException
    {
        this.databaseReader = new DatabaseReader.Builder(databasePath.toFile())
                .withCache(new CHMCache())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CityResponse city(InetAddress address) throws IOException, GeoIp2Exception
    {
        return databaseReader.city(address);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        databaseReader.close();
    }
}
