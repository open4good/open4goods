package org.open4goods.commons.model.icecat;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public class IcecatSupplierName {

    @JacksonXmlProperty(isAttribute = true)
    private Integer ID;

    @JacksonXmlProperty(isAttribute = true)
    private Integer langid;

    @JacksonXmlProperty(isAttribute = true)
    private String Value;

    @JacksonXmlProperty(isAttribute = true)
    private String Name;

    @JacksonXmlProperty(isAttribute = true)
    private String Updated;

    @JacksonXmlText
    private String content;

    // Getters et setters

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public Integer getLangid() {
        return langid;
    }

    public void setLangid(Integer langid) {
        this.langid = langid;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        Value = value;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getUpdated() {
        return Updated;
    }

    public void setUpdated(String updated) {
        Updated = updated;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
