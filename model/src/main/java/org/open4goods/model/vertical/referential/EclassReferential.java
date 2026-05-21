package org.open4goods.model.vertical.referential;

/**
 * An eCl@ss classification reference for a vertical.
 * <p>
 * eCl@ss is a cross-industry standard for technical and industrial specifications.
 * This field is reserved for future integration.
 */
public class EclassReferential
{
    /** eCl@ss class identifier (e.g. "22410101"). */
    private String classId;

    /** English class name for documentation (e.g. "Air conditioner (split system)"). */
    private String className;

    public EclassReferential()
    {
    }

    public EclassReferential(String classId, String className)
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
