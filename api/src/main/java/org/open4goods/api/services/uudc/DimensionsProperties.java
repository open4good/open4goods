package org.open4goods.api.services.uudc;

import java.util.HashMap;
import java.util.Map;

/**
 * Top-level YAML-bound wrapper for {@code dimensions.yml}.
 * <p>
 * Expected YAML structure:
 * <pre>
 * dimensions:
 *   MASS:
 *     baseUnit: "kg"
 *     units: [...]
 *   LENGTH:
 *     ...
 * </pre>
 */
public class DimensionsProperties
{
    /** Map of dimension name (e.g. "MASS") to its configuration. */
    private Map<String, DimensionConfig> dimensions = new HashMap<>();

    public DimensionsProperties()
    {
    }

    public Map<String, DimensionConfig> getDimensions()
    {
        return dimensions;
    }

    public void setDimensions(Map<String, DimensionConfig> dimensions)
    {
        this.dimensions = dimensions == null ? new HashMap<>() : dimensions;
    }
}
