package org.open4goods.model.vertical.referential;

/**
 * A Google Product Taxonomy entry associated with a vertical.
 * <p>
 * Multiple entries allow a vertical to span several Google taxonomy leaves.
 */
public class GoogleTaxonomyReferential
{
    /** Numeric Google taxonomy identifier (e.g. 605 for "Air Conditioners"). */
    private Integer id;

    /** Full taxonomy path for documentation (e.g. "Home &amp; Garden > ... > Air Conditioners"). */
    private String name;

    public GoogleTaxonomyReferential()
    {
    }

    public GoogleTaxonomyReferential(Integer id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
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
