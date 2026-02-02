package org.open4goods.services.geocode.service.geonames;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.open4goods.services.geocode.model.CityRecord;
import org.springframework.stereotype.Component;

/**
 * Parses GeoNames tab-separated lines into city records.
 */
@Component
public class GeoNamesParser
{
    private static final int INDEX_GEONAME_ID = 0;
    private static final int INDEX_NAME = 1;
    private static final int INDEX_ASCII_NAME = 2;
    private static final int INDEX_ALTERNATE_NAMES = 3;
    private static final int INDEX_LATITUDE = 4;
    private static final int INDEX_LONGITUDE = 5;
    private static final int INDEX_FEATURE_CLASS = 6;
    private static final int INDEX_COUNTRY_CODE = 8;
    private static final int INDEX_POPULATION = 14;

    /**
     * Parses a TSV line into a {@link GeoNamesEntry} structure.
     *
     * @param line input line
     * @return parsed entry or null if required fields are missing
     */
    public GeoNamesEntry parseLine(String line)
    {
        String[] parts = line.split("\t", -1);
        if (parts.length <= INDEX_COUNTRY_CODE)
        {
            return null;
        }
        String geonameIdText = get(parts, INDEX_GEONAME_ID);
        String name = get(parts, INDEX_NAME);
        String asciiName = get(parts, INDEX_ASCII_NAME);
        String countryCode = get(parts, INDEX_COUNTRY_CODE);
        String latitudeText = get(parts, INDEX_LATITUDE);
        String longitudeText = get(parts, INDEX_LONGITUDE);
        String featureClass = get(parts, INDEX_FEATURE_CLASS);
        if (geonameIdText.isEmpty() || name.isEmpty() || countryCode.isEmpty()
                || latitudeText.isEmpty() || longitudeText.isEmpty())
        {
            return null;
        }

        long geonameId = parseLong(geonameIdText);
        double latitude = parseDouble(latitudeText);
        double longitude = parseDouble(longitudeText);
        long population = parseLong(get(parts, INDEX_POPULATION));
        List<String> alternates = parseAlternates(get(parts, INDEX_ALTERNATE_NAMES));

        CityRecord record = new CityRecord(
                geonameId,
                name,
                asciiName,
                countryCode,
                latitude,
                longitude,
                population,
                featureClass);
        return new GeoNamesEntry(record, alternates);
    }

    private String get(String[] parts, int index)
    {
        if (index >= parts.length)
        {
            return "";
        }
        return parts[index];
    }

    private long parseLong(String value)
    {
        if (value == null || value.isBlank())
        {
            return 0L;
        }
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException ex)
        {
            return 0L;
        }
    }

    private double parseDouble(String value)
    {
        if (value == null || value.isBlank())
        {
            return 0d;
        }
        try
        {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException ex)
        {
            return 0d;
        }
    }

    private List<String> parseAlternates(String value)
    {
        if (value == null || value.isBlank())
        {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .filter(entry -> !entry.isBlank())
                .toList();
    }
}

record GeoNamesEntry(CityRecord record, List<String> alternateNames)
{
}
