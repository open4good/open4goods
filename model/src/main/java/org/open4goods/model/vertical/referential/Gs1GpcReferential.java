package org.open4goods.model.vertical.referential;

/**
 * A GS1 Global Product Classification (GPC) brick reference for a vertical.
 * <p>
 * GS1 GPC is used in retail and consumer packaged goods for GTIN/barcode alignment.
 * This field is reserved for future integration.
 */
public class Gs1GpcReferential
{
    /** GS1 GPC brick code (e.g. "10001402"). */
    private String brickCode;

    /** English brick name for documentation (e.g. "Air Conditioning/Cooling Equipment"). */
    private String brickName;

    public Gs1GpcReferential()
    {
    }

    public Gs1GpcReferential(String brickCode, String brickName)
    {
        this.brickCode = brickCode;
        this.brickName = brickName;
    }

    public String getBrickCode()
    {
        return brickCode;
    }

    public void setBrickCode(String brickCode)
    {
        this.brickCode = brickCode;
    }

    public String getBrickName()
    {
        return brickName;
    }

    public void setBrickName(String brickName)
    {
        this.brickName = brickName;
    }
}
