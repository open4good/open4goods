package org.open4goods.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatFeatureGroupsList {

    @JacksonXmlProperty(localName = "FeatureGroup")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<IcecatFeatureGroup> featureGroups;

    // Getters et setters

    public List<IcecatFeatureGroup> getFeatureGroups() {
        return featureGroups;
    }

    public void setFeatureGroups(List<IcecatFeatureGroup> featureGroups) {
        this.featureGroups = featureGroups;
    }
}