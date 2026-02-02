package org.open4goods.services.geocode.service;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;

import org.open4goods.services.geocode.service.geonames.GeoNamesDatasetProvider;
import org.open4goods.services.geocode.service.geonames.GeoNamesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Loads the GeoNames dataset into memory during application startup.
 */
@Service
public class GeoNamesIndexService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesIndexService.class);

    private final GeoNamesDatasetProvider datasetProvider;
    private final GeoNamesLoader loader;
    private final AtomicReference<CityIndex> indexRef = new AtomicReference<>();

    /**
     * Creates a new index service.
     *
     * @param datasetProvider dataset provider
     * @param loader GeoNames loader
     */
    public GeoNamesIndexService(GeoNamesDatasetProvider datasetProvider, GeoNamesLoader loader)
    {
        this.datasetProvider = datasetProvider;
        this.loader = loader;
    }

    /**
     * Loads the dataset at startup.
     */
    @PostConstruct
    public void initialize()
    {
        Path datasetPath = datasetProvider.getDatasetPath();
        CityIndex index = loader.load(datasetPath);
        if (index.getIndexSize() == 0)
        {
            throw new IllegalStateException("GeoNames index is empty after loading");
        }
        indexRef.set(index);
        LOGGER.info("GeoNames index ready: {} records", index.getRecordCount());
    }

    /**
     * Returns the loaded city index.
     *
     * @return city index
     */
    public CityIndex getIndex()
    {
        CityIndex index = indexRef.get();
        if (index == null)
        {
            throw new IllegalStateException("GeoNames index not loaded");
        }
        return index;
    }

    /**
     * Returns whether the index has been loaded.
     *
     * @return true when loaded
     */
    public boolean isLoaded()
    {
        CityIndex index = indexRef.get();
        return index != null && index.getIndexSize() > 0;
    }
}
