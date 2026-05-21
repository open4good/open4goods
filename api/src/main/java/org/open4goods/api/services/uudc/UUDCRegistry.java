package org.open4goods.api.services.uudc;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Unified Unit Dimension &amp; Conversion (UUDC) Registry.
 * <p>
 * Loads physical dimension definitions from {@code dimensions.yml} on the
 * classpath at startup and exposes:
 * <ul>
 *   <li>{@link #resolveUnit(String, String)} — resolve a raw unit symbol to its
 *       {@link UnitDefinition} within a given dimension.</li>
 *   <li>{@link #convertToBase(double, String, String)} — convert a raw value in a
 *       named unit to the dimension's base unit.</li>
 * </ul>
 * Synonym lookup is case-insensitive and normalized. When a symbol cannot be resolved,
 * a WARN is logged and {@code null} / the unchanged value is returned, allowing callers
 * to fall back gracefully.
 */
@Service
public class UUDCRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UUDCRegistry.class);

    private static final String DIMENSIONS_RESOURCE = "/dimensions.yml";

    private final SerialisationService serialisationService;

    /**
     * Outer key: uppercase dimension name (e.g. "LENGTH").
     * Inner key: lowercase-normalized synonym.
     */
    private Map<String, Map<String, UnitDefinition>> dimensionSynonymIndex = new HashMap<>();

    /** Stores the base unit symbol for each dimension. */
    private Map<String, String> baseUnits = new HashMap<>();

    public UUDCRegistry(SerialisationService serialisationService)
    {
        this.serialisationService = serialisationService;
    }

    /**
     * Loads and indexes {@code dimensions.yml} from the classpath.
     *
     * @throws IllegalStateException if the file cannot be loaded or parsed
     */
    @PostConstruct
    public void init()
    {
        try (InputStream is = UUDCRegistry.class.getResourceAsStream(DIMENSIONS_RESOURCE))
        {
            if (is == null)
            {
                LOGGER.error("dimensions.yml not found on classpath at {}", DIMENSIONS_RESOURCE);
                return;
            }
            DimensionsProperties props = serialisationService.fromYaml(is, DimensionsProperties.class);
            if (props == null || props.getDimensions() == null)
            {
                LOGGER.error("dimensions.yml loaded but contained no dimension definitions");
                return;
            }
            buildIndex(props.getDimensions());
            LOGGER.info("UUDC registry loaded: {} dimensions", dimensionSynonymIndex.size());
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to load dimensions.yml: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolves a raw unit symbol (e.g. "kg", "kilogrammes", "CM") to its
     * {@link UnitDefinition} within the given dimension.
     *
     * @param rawSymbol raw unit string from the data source (case-insensitive)
     * @param dimension uppercase dimension name, e.g. "MASS" or "LENGTH"
     * @return the matching {@link UnitDefinition}, or {@code null} if not found
     */
    public UnitDefinition resolveUnit(String rawSymbol, String dimension)
    {
        if (rawSymbol == null || dimension == null)
        {
            return null;
        }
        Map<String, UnitDefinition> index = dimensionSynonymIndex.get(dimension.toUpperCase(Locale.ROOT));
        if (index == null)
        {
            LOGGER.warn("Unknown dimension '{}' in UUDC registry", dimension);
            return null;
        }
        String normalized = rawSymbol.trim().toLowerCase(Locale.ROOT);
        UnitDefinition def = index.get(normalized);
        if (def == null)
        {
            LOGGER.warn("Unresolved unit symbol '{}' in dimension '{}'", rawSymbol, dimension);
        }
        return def;
    }

    /**
     * Converts a raw value in {@code fromSymbol} units to the base unit of the
     * given dimension.
     * <p>
     * Formula: {@code base_value = raw_value * multiplier + offset}
     *
     * @param value     the raw numeric value
     * @param fromSymbol the unit of the raw value (case-insensitive)
     * @param dimension  the dimension name (e.g. "LENGTH")
     * @return the value in the dimension's base unit, or {@code value} unchanged
     *         when the symbol cannot be resolved
     */
    public double convertToBase(double value, String fromSymbol, String dimension)
    {
        UnitDefinition def = resolveUnit(fromSymbol, dimension);
        if (def == null)
        {
            return value;
        }
        return value * def.getMultiplier() + def.getOffset();
    }

    /**
     * Returns the base unit symbol for the given dimension, or {@code null} if unknown.
     *
     * @param dimension uppercase dimension name
     * @return base unit symbol (e.g. "m" for LENGTH)
     */
    public String getBaseUnit(String dimension)
    {
        return dimension == null ? null : baseUnits.get(dimension.toUpperCase(Locale.ROOT));
    }

    /**
     * Returns an unmodifiable view of all registered dimensions.
     *
     * @return map of dimension name → synonym-index
     */
    public Map<String, Map<String, UnitDefinition>> getDimensionSynonymIndex()
    {
        return Collections.unmodifiableMap(dimensionSynonymIndex);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void buildIndex(Map<String, DimensionConfig> dimensions)
    {
        dimensionSynonymIndex = new HashMap<>();
        baseUnits = new HashMap<>();

        for (Map.Entry<String, DimensionConfig> entry : dimensions.entrySet())
        {
            String dimensionName = entry.getKey().toUpperCase(Locale.ROOT);
            DimensionConfig cfg = entry.getValue();
            Map<String, UnitDefinition> synonymIndex = new HashMap<>();

            if (cfg.getBaseUnit() != null)
            {
                baseUnits.put(dimensionName, cfg.getBaseUnit());
            }

            List<UnitDefinition> units = cfg.getUnits();
            if (units == null)
            {
                continue;
            }
            for (UnitDefinition unit : units)
            {
                if (unit.getSymbol() != null)
                {
                    synonymIndex.put(unit.getSymbol().toLowerCase(Locale.ROOT), unit);
                }
                if (unit.getSynonyms() != null)
                {
                    for (String synonym : unit.getSynonyms())
                    {
                        if (synonym != null && !synonym.isBlank())
                        {
                            synonymIndex.put(synonym.trim().toLowerCase(Locale.ROOT), unit);
                        }
                    }
                }
            }
            dimensionSynonymIndex.put(dimensionName, synonymIndex);
            LOGGER.debug("UUDC dimension '{}': {} synonym entries", dimensionName, synonymIndex.size());
        }
    }
}
