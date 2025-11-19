package org.open4goods.icecat.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatEANCode {
    @JacksonXmlProperty(isAttribute = true)
    private String EAN;

    public String getEAN() {
        return EAN;
    }

    public void setEAN(String EAN) {
        this.EAN = EAN;
    }
}
