package org.open4goods.model.vertical.referential;

/**
 * An Icecat category reference associated with a vertical.
 * <p>
 * Multiple entries are allowed when a vertical maps to several Icecat categories.
 */
public class IcecatReferential
{
    /** Icecat numeric category identifier. */
    private Integer categoryId;

    /** English category name for documentation purposes. */
    private String name;

    public IcecatReferential()
    {
    }

    public IcecatReferential(Integer categoryId, String name)
    {
        this.categoryId = categoryId;
        this.name = name;
    }

    public Integer getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId)
    {
        this.categoryId = categoryId;
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
