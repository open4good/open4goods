package org.open4goods.model.icecat;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "CategoryFeatureGroup")
public class IcecatCategoryFeatureGroup {

    @JacksonXmlProperty(isAttribute = true)
    private Integer No;

    @JacksonXmlProperty(isAttribute = true)
    private Integer ID;

    @JacksonXmlProperty(isAttribute = true)
    private String FeatureGroup;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "FeatureGroup")
    private List<IcecatFeatureGroup> featureGroups;

    // Getters et setters

    public Integer getNo() {
        return No;
    }

    public void setNo(Integer no) {
        No = no;
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public String getFeatureGroup() {
        return FeatureGroup;
    }

    public void setFeatureGroup(String featureGroup) {
        FeatureGroup = featureGroup;
    }

    public List<IcecatFeatureGroup> getFeatureGroups() {
        return featureGroups;
    }

    public void setFeatureGroups(List<IcecatFeatureGroup> featureGroups) {
        this.featureGroups = featureGroups;
    }


}
