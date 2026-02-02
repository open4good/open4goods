package org.open4goods.services.geocode.service.geonames;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.open4goods.services.geocode.model.CityMatch;
import org.open4goods.services.geocode.model.CityRecord;
import org.open4goods.services.geocode.model.MatchType;
import org.open4goods.services.geocode.service.CityIndex;
import org.open4goods.services.geocode.util.NormalizationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Loads and indexes GeoNames city data into memory.
 */
@Component
public class GeoNamesLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesLoader.class);

    private final GeoNamesParser parser;

    /**
     * Creates a new loader with the given parser.
     *
     * @param parser GeoNames parser
     */
    public GeoNamesLoader(GeoNamesParser parser)
    {
        this.parser = parser;
    }

    /**
     * Loads and indexes the GeoNames dataset into memory.
     *
     * @param datasetPath path to the cities file
     * @return built city index
     */
    public CityIndex load(Path datasetPath)
    {
        Objects.requireNonNull(datasetPath, "datasetPath");
        Instant start = Instant.now();
        long recordCount = 0L;
        Map<String, CityMatch> index = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(datasetPath, StandardCharsets.UTF_8))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                GeoNamesEntry entry = parser.parseLine(line);
                if (entry == null)
                {
                    continue;
                }
                recordCount++;
                CityRecord record = entry.record();
                String countryCode = record.countryCode().toUpperCase();

                addCandidate(index, record.name(), countryCode, record, MatchType.PRIMARY);
                if (!record.asciiName().isBlank() && !record.asciiName().equals(record.name()))
                {
                    addCandidate(index, record.asciiName(), countryCode, record, MatchType.ASCII);
                }
                for (String alternate : entry.alternateNames())
                {
                    addCandidate(index, alternate, countryCode, record, MatchType.ALTERNATE);
                }
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Failed to load GeoNames dataset from " + datasetPath, ex);
        }

        Duration duration = Duration.between(start, Instant.now());
        LOGGER.info("Loaded {} GeoNames records into {} lookup keys in {} ms",
                recordCount, index.size(), duration.toMillis());
        return new CityIndex(index, recordCount);
    }

    private void addCandidate(Map<String, CityMatch> index, String cityName, String countryCode,
            CityRecord record, MatchType matchType)
    {
        String normalizedCity = NormalizationUtil.normalize(cityName);
        if (normalizedCity.isBlank())
        {
            return;
        }
        // Using a single composite key keeps lookup O(1) while avoiding nested maps.
        String key = normalizedCity + "|" + countryCode;

        CityMatch existing = index.get(key);
        CityMatch candidate = new CityMatch(record, matchType);
        if (existing == null || isBetterCandidate(candidate, existing))
        {
            index.put(key, candidate);
        }
    }

    private boolean isBetterCandidate(CityMatch candidate, CityMatch existing)
    {
        CityRecord candidateRecord = candidate.record();
        CityRecord existingRecord = existing.record();
        if (candidateRecord.population() > existingRecord.population())
        {
            return true;
        }
        if (candidateRecord.population() < existingRecord.population())
        {
            return false;
        }
        int candidateScore = featureClassScore(candidateRecord.featureClass());
        int existingScore = featureClassScore(existingRecord.featureClass());
        if (candidateScore != existingScore)
        {
            return candidateScore > existingScore;
        }
        // Keep the first encountered entry when all tie-breakers are equal.
        return false;
    }

    private int featureClassScore(String featureClass)
    {
        // Prefer populated places (feature class "P") when population is equal.
        return "P".equalsIgnoreCase(featureClass) ? 1 : 0;
    }
}
