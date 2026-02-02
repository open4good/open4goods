package org.open4goods.services.geocode.service.maxmind;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

/**
 * Default factory creating MaxMind GeoIP readers.
 */
@Component
public class DefaultGeoIpDatabaseReaderFactory implements GeoIpDatabaseReaderFactory
{
    /**
     * {@inheritDoc}
     */
    @Override
    public GeoIpDatabaseReader create(Path databasePath) throws IOException
    {
        return new MaxMindGeoIpDatabaseReader(databasePath);
    }
}
