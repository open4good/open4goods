package org.open4goods.api.services.uudc;

import java.util.ArrayList;
import java.util.List;

/**
 * A unit of measure within a physical dimension, with its conversion factor to the
 * dimension's base unit and a list of recognized input synonyms.
 * <p>
 * Conversion: {@code value_in_base = raw_value * multiplier + offset}
 * <p>
 * For most units the offset is 0. Temperature Celsius-to-Kelvin would use offset=273.15,
 * but that dimension is not registered by default.
 */
public class UnitDefinition
{
    /** Canonical symbol used in serialization and display (e.g. "kg", "cm"). */
    private String symbol;

    /**
     * Multiplication factor converting this unit to the dimension's base unit.
     * For the base unit itself, {@code multiplier = 1.0}.
     */
    private double multiplier = 1.0;

    /**
     * Additive offset applied after multiplying. Zero for all non-temperature dimensions.
     */
    private double offset = 0.0;

    /** True when this unit IS the base unit for its dimension. */
    private boolean isBase = false;

    /**
     * Case-insensitive synonyms (alternative spellings, abbreviations, locale variants)
     * that are recognized during parsing and resolved to this unit.
     */
    private List<String> synonyms = new ArrayList<>();

    public UnitDefinition()
    {
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public double getMultiplier()
    {
        return multiplier;
    }

    public void setMultiplier(double multiplier)
    {
        this.multiplier = multiplier;
    }

    public double getOffset()
    {
        return offset;
    }

    public void setOffset(double offset)
    {
        this.offset = offset;
    }

    public boolean isBase()
    {
        return isBase;
    }

    public void setBase(boolean base)
    {
        isBase = base;
    }

    public List<String> getSynonyms()
    {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms)
    {
        this.synonyms = synonyms == null ? new ArrayList<>() : synonyms;
    }
}
