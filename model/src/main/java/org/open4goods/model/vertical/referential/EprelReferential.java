package org.open4goods.model.vertical.referential;

/**
 * An EPREL (European Product Registry for Energy Labelling) product group
 * reference for a vertical.
 */
public class EprelReferential
{
    /** EPREL product group identifier (e.g. "airconditioners"). */
    private String group;

    public EprelReferential()
    {
    }

    public EprelReferential(String group)
    {
        this.group = group;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(String group)
    {
        this.group = group;
    }
}
