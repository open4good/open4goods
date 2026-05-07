package org.open4goods.icecat.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** Reference to a parent category in the Icecat category hierarchy. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "ParentCategory")
public class IcecatParentCategory {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Names")
    private List<IcecatNames> namesList;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public List<IcecatNames> getNamesList() { return namesList; }
    public void setNamesList(List<IcecatNames> namesList) { this.namesList = namesList; }
}
