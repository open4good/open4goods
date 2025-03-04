package org.open4goods.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "ParentCategory")
public class IcecatParentCategory {

    @JacksonXmlProperty(isAttribute = true)
    private Integer ID;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Names")
    private List<IcecatNames> namesList;

    // Getters and setters

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public List<IcecatNames> getNamesList() {
        return namesList;
    }

    public void setNamesList(List<IcecatNames> namesList) {
        this.namesList = namesList;
    }
}