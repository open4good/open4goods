package org.open4goods.model.vertical.referential;

/**
 * An ETIM feature reference associated with a single attribute.
 * <p>
 * ETIM features carry an {@code EFxxxxxx} identifier scoped under an ETIM
 * class (held at vertical level in {@link TaxonomyReferentials#getEtim()}).
 * Multiple entries are valid when the Nudger attribute aggregates several
 * physical ETIM features.
 */
public class EtimFeatureReferential
{
    /** ETIM feature identifier (e.g. "EF000003"). */
    private String featureId;

    /** English feature name for documentation purposes (e.g. "Height"). */
    private String name;

    /** Optional ETIM unit identifier ({@code EUxxxxxx}) or unit symbol. */
    private String unit;

    public EtimFeatureReferential()
    {
    }

    public EtimFeatureReferential(String featureId, String name, String unit)
    {
        this.featureId = featureId;
        this.name = name;
        this.unit = unit;
    }

    public String getFeatureId()
    {
        return featureId;
    }

    public void setFeatureId(String featureId)
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
