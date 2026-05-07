package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Wrapper for the {@code <FeaturesList>} response element. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatFeaturesList {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Feature")
    private List<IcecatFeature> features;

    public List<IcecatFeature> getFeatures() {
        return features != null ? features : Collections.emptyList();
    }

    public void setFeatures(List<IcecatFeature> features) {
        this.features = features;
    }
}
