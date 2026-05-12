package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Wrapper for the {@code <FeatureGroupsList>} response element. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatFeatureGroupsList {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "FeatureGroup")
    private List<IcecatFeatureGroup> featureGroups;

    public List<IcecatFeatureGroup> getFeatureGroups() {
        return featureGroups != null ? featureGroups : Collections.emptyList();
    }

    public void setFeatureGroups(List<IcecatFeatureGroup> featureGroups) {
        this.featureGroups = featureGroups;
    }
}
