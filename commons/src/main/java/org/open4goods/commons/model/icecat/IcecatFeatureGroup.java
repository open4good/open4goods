package org.open4goods.commons.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "FeatureGroup")
public class IcecatFeatureGroup {

    @JacksonXmlProperty(isAttribute = true)
    private Integer ID;

    @JacksonXmlProperty(isAttribute = true)
    private Integer sid;

    @JacksonXmlProperty(isAttribute = true)
    private String Updated;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Name")
    private List<IcecatName> names;

    // Getters et setters

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public String getUpdated() {
        return Updated;
    }

    public void setUpdated(String updated) {
        Updated = updated;
    }

    public List<IcecatName> getNames() {
        return names;
    }

    public void setNames(List<IcecatName> names) {
        this.names = names;
    }
}
