package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;

/** A localized supplier (brand) name entry from the Icecat catalog. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatSupplierName {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "langid")
    private Integer langId;

    @JacksonXmlProperty(isAttribute = true, localName = "Value")
    private String value;

    @JacksonXmlProperty(isAttribute = true, localName = "Name")
    private String name;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    @JacksonXmlText
    private String content;

    public String getEffectiveName() {
        if (name != null) return name;
        if (value != null) return value;
        return content;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getLangId() { return langId; }
    public void setLangId(Integer langId) { this.langId = langId; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
