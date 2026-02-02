package org.open4goods.services.geocode.service.maxmind;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Factory for creating GeoIP database readers.
 */
@FunctionalInterface
public interface GeoIpDatabaseReaderFactory
{
    /**
     * Creates a reader for the given database path.
     *
     * @param databasePath path to the database file
     * @return database reader
     * @throws IOException when the database cannot be opened
     */
    GeoIpDatabaseReader create(Path databasePath) throws IOException;
}
