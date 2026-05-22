package org.open4goods.model.vertical.referential;

/**
 * An Icecat feature reference associated with a single attribute.
 * <p>
 * Multiple entries are allowed when several Icecat features correspond to the
 * same logical Nudger attribute (e.g. {@code Height} appears under feature
 * IDs 1464 and 5478 across product categories).
 */
public class IcecatFeatureReferential
{
    /** Icecat feature identifier (e.g. 1464). */
    private Integer featureId;

    /** English feature name for documentation purposes (e.g. "Height"). */
    private String name;

    /** Optional unit reported by Icecat for that feature (e.g. "mm", "kg"). */
    private String unit;

    public IcecatFeatureReferential()
    {
    }

    public IcecatFeatureReferential(Integer featureId, String name, String unit)
    {
        this.featureId = featureId;
        this.name = name;
        this.unit = unit;
    }

    public Integer getFeatureId()
    {
        return featureId;
    }

    public void setFeatureId(Integer featureId)
    {
        this.featureId = featureId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }
}
