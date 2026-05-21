package org.open4goods.model.vertical.referential;

/**
 * An ETIM (European Technical Information Model) class reference for a vertical.
 * <p>
 * ETIM is a technical classification standard widely used in HVAC, electrical,
 * and building products. Multiple entries are allowed when a vertical covers
 * several ETIM classes (e.g. split vs portable air conditioners).
 */
public class EtimReferential
{
    /** ETIM class identifier (e.g. "EC011604"). */
    private String classId;

    /** English class name for documentation purposes (e.g. "Portable air conditioner"). */
    private String className;

    public EtimReferential()
    {
    }

    public EtimReferential(String classId, String className)
    {
        this.classId = classId;
        this.className = className;
    }

    public String getClassId()
    {
        return classId;
    }

    public void setClassId(String classId)
    {
        this.classId = classId;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }
}
