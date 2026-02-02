package org.open4goods.services.geocode.service;

import java.util.Locale;

import org.open4goods.services.geocode.model.CityMatch;
import org.open4goods.services.geocode.util.NormalizationUtil;
import org.springframework.stereotype.Service;

/**
 * Default geocode service implementation backed by an in-memory index.
 */
@Service
public class GeocodeServiceImpl implements GeocodeService
{
    private final GeoNamesIndexService indexService;

    /**
     * Creates a new geocode service.
     *
     * @param indexService index service
     */
    public GeocodeServiceImpl(GeoNamesIndexService indexService)
    {
        this.indexService = indexService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CityMatch resolve(String city, String countryCode)
    {
        String normalizedCity = NormalizationUtil.normalize(city);
        String normalizedCountry = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
        String key = normalizedCity + "|" + normalizedCountry;
        return indexService.getIndex().findByKey(key);
    }
}
