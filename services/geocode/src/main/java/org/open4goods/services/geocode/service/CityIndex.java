package org.open4goods.services.geocode.service;

import java.util.Map;
import java.util.Objects;

import org.open4goods.services.geocode.model.CityMatch;

/**
 * Immutable in-memory index for geocode lookups.
 */
public class CityIndex
{
    private final Map<String, CityMatch> cityByKey;
    private final long recordCount;

    /**
     * Creates a new city index instance.
     *
     * @param cityByKey indexed city matches
     * @param recordCount number of GeoNames records loaded
     */
    public CityIndex(Map<String, CityMatch> cityByKey, long recordCount)
    {
        this.cityByKey = Map.copyOf(cityByKey);
        this.recordCount = recordCount;
    }

    /**
     * Finds a city match by lookup key.
     *
     * @param key normalized lookup key
     * @return city match or null when not found
     */
    public CityMatch findByKey(String key)
    {
        Objects.requireNonNull(key, "key");
        return cityByKey.get(key);
    }

    /**
     * Returns the number of indexed lookup entries.
     *
     * @return index size
     */
    public int getIndexSize()
    {
        return cityByKey.size();
    }

    /**
     * Returns the number of GeoNames records loaded into memory.
     *
     * @return record count
     */
    public long getRecordCount()
    {
        return recordCount;
    }
}
