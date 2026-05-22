package org.open4goods.model.vertical.referential;

/**
 * An EPREL feature reference associated with a single attribute.
 * <p>
 * EPREL feature names are dependent on the product group (e.g.
 * {@code dimensionHeight} appears in the {@code televisions} group while
 * {@code height} is the equivalent in the {@code refrigeratingappliances}
 * group). The optional {@code productGroup} field documents the binding.
 */
public class EprelFeatureReferential
{
    /** EPREL feature name (e.g. "dimensionHeight"). */
    private String featureName;

    /**
     * Optional EPREL product group hosting this feature (e.g. "televisions").
     * <p>
     * Documentary only; the runtime EPREL resolution uses the vertical-level
     * {@code eprelGroupNames} list.
     */
    private String productGroup;

    /** English feature label for documentation purposes (e.g. "Height in mm"). */
    private String name;

    public EprelFeatureReferential()
    {
    }

    public EprelFeatureReferential(String featureName, String productGroup, String name)
    {
        this.featureName = featureName;
        this.productGroup = productGroup;
        this.name = name;
    }

    public String getFeatureName()
    {
        return featureName;
    }

    public void setFeatureName(String featureName)
    {
        this.featureName = featureName;
    }

    public String getProductGroup()
    {
        return productGroup;
    }

    public void setProductGroup(String productGroup)
    {
        this.productGroup = productGroup;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
