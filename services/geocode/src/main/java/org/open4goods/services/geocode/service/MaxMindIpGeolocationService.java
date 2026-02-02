package org.open4goods.services.geocode.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.open4goods.services.geocode.dto.IpGeolocationResponse;
import org.open4goods.services.geocode.service.maxmind.GeoIpDatabaseReader;
import org.open4goods.services.geocode.service.maxmind.GeoIpDatabaseReaderFactory;
import org.open4goods.services.geocode.service.maxmind.MaxMindDatasetProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * MaxMind-backed implementation of the IP geolocation service.
 */
@Service
public class MaxMindIpGeolocationService implements IpGeolocationService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MaxMindIpGeolocationService.class);

    private final MaxMindDatasetProvider datasetProvider;
    private final GeoIpDatabaseReaderFactory readerFactory;
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private GeoIpDatabaseReader reader;

    /**
     * Creates a new MaxMind geolocation service.
     *
     * @param datasetProvider dataset provider
     * @param readerFactory database reader factory
     */
    public MaxMindIpGeolocationService(MaxMindDatasetProvider datasetProvider,
            GeoIpDatabaseReaderFactory readerFactory)
    {
        this.datasetProvider = datasetProvider;
        this.readerFactory = readerFactory;
    }

    /**
     * Loads the GeoIP database on startup.
     */
    @PostConstruct
    public void initialize()
    {
        loadDatabase();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IpGeolocationResponse resolve(String ip)
    {
        Objects.requireNonNull(ip, "ip");
        if (!loaded.get())
        {
            loadDatabase();
        }
        InetAddress address = parseAddress(ip);
        if (address == null)
        {
            return null;
        }
        try
        {
            CityResponse response = reader.city(address);
            return toResponse(ip, response);
        }
        catch (AddressNotFoundException ex)
        {
            return null;
        }
        catch (IOException | GeoIp2Exception ex)
        {
            throw new IllegalStateException("Failed to resolve IP geolocation", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoaded()
    {
        return loaded.get();
    }

    /**
     * Closes the GeoIP database reader.
     */
    @PreDestroy
    public void shutdown()
    {
        if (reader != null)
        {
            try
            {
                reader.close();
            }
            catch (IOException ex)
            {
                LOGGER.warn("Failed to close GeoIP database reader", ex);
            }
        }
    }

    /**
     * Loads or reloads the GeoIP database from disk.
     */
    private void loadDatabase()
    {
        Instant start = Instant.now();
        try
        {
            GeoIpDatabaseReader newReader = readerFactory.create(datasetProvider.getDatabasePath());
            closeReader();
            reader = newReader;
            loaded.set(true);
            Duration duration = Duration.between(start, Instant.now());
            LOGGER.info("Loaded MaxMind GeoIP database in {} ms", duration.toMillis());
        }
        catch (IOException ex)
        {
            loaded.set(false);
            throw new IllegalStateException("Failed to load MaxMind GeoIP database", ex);
        }
    }

    /**
     * Closes the current reader if present.
     *
     * @throws IOException when closing fails
     */
    private void closeReader() throws IOException
    {
        if (reader != null)
        {
            reader.close();
        }
    }

    /**
     * Parses the string IP into an {@link InetAddress}.
     *
     * @param ip IP address string
     * @return parsed address or null when invalid
     */
    private InetAddress parseAddress(String ip)
    {
        try
        {
            return InetAddress.getByName(ip);
        }
        catch (UnknownHostException ex)
        {
            LOGGER.warn("Invalid IP address provided: {}", ip);
            return null;
        }
    }

    /**
     * Maps MaxMind responses to the API DTO.
     *
     * @param ip IP address
     * @param response MaxMind response
     * @return geolocation response DTO
     */
    private IpGeolocationResponse toResponse(String ip, CityResponse response)
    {
        Location location = response.getLocation();
        Subdivision subdivision = response.getSubdivisions().isEmpty()
                ? null
                : response.getSubdivisions().getFirst();
        Integer accuracyRadius = location == null ? null : location.getAccuracyRadius();
        return new IpGeolocationResponse(
                ip,
                response.getContinent().getName(),
                response.getContinent().getCode(),
                response.getCountry().getName(),
                response.getCountry().getIsoCode(),
                response.getRegisteredCountry().getName(),
                response.getRegisteredCountry().getIsoCode(),
                response.getCity().getName(),
                subdivision == null ? null : subdivision.getName(),
                subdivision == null ? null : subdivision.getIsoCode(),
                response.getPostal().getCode(),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                accuracyRadius,
                location == null ? null : location.getTimeZone(),
                location == null ? null : location.getMetroCode(),
                response.getTraits().isAnonymousProxy(),
                response.getTraits().isAnycast());
    }
}
