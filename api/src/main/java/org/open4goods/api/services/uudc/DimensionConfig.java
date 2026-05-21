package org.open4goods.api.services.uudc;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML-bound configuration for a single physical dimension (e.g. LENGTH, MASS).
 * <p>
 * Loaded by {@link UUDCRegistry} from {@code dimensions.yml} on startup.
 */
public class DimensionConfig
{
    /** Symbol of the canonical base unit for this dimension (e.g. "kg", "m"). */
    private String baseUnit;

    /** All units that can be converted to the base unit. */
    private List<UnitDefinition> units = new ArrayList<>();

    public DimensionConfig()
    {
    }

    public String getBaseUnit()
    {
        return baseUnit;
    }

    public void setBaseUnit(String baseUnit)
    {
        this.baseUnit = baseUnit;
    }

    public List<UnitDefinition> getUnits()
    {
        return units;
    }

    public void setUnits(List<UnitDefinition> units)
    {
        this.units = units == null ? new ArrayList<>() : units;
    }
}
