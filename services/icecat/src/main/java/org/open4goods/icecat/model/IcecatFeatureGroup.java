package org.open4goods.icecat.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** A logical grouping of related features in the Icecat taxonomy (e.g. "Display", "Connectivity"). */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "FeatureGroup")
public class IcecatFeatureGroup {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "sid")
    private Integer sid;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Name")
    private List<IcecatName> names;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getSid() { return sid; }
    public void setSid(Integer sid) { this.sid = sid; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public List<IcecatName> getNames() { return names; }
    public void setNames(List<IcecatName> names) { this.names = names; }
}
