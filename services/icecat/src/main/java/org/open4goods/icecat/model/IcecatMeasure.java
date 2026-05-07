package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Unit-of-measure definition from the Icecat catalog (e.g. kg, MHz, GB). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatMeasure {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "Sign")
    private String sign;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    @JacksonXmlProperty(localName = "Signs")
    private IcecatSigns signs;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public IcecatSigns getSigns() { return signs; }
    public void setSigns(IcecatSigns signs) { this.signs = signs; }
}
