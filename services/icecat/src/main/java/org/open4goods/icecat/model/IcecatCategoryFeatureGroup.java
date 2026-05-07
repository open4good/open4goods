package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Links a category to one or more feature groups in the Icecat taxonomy.
 *
 * <p>Note: the Icecat XML uses the name {@code FeatureGroup} both as an XML attribute
 * (carrying a string label) and as child element names (the actual feature group entries).
 * They are mapped to separate fields here to avoid ambiguity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "CategoryFeatureGroup")
public class IcecatCategoryFeatureGroup {

    @JacksonXmlProperty(isAttribute = true, localName = "No")
    private Integer no;

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    /** The {@code FeatureGroup} XML attribute (a string label, distinct from the child elements). */
    @JacksonXmlProperty(isAttribute = true, localName = "FeatureGroup")
    private String featureGroupLabel;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "FeatureGroup")
    private List<IcecatFeatureGroup> featureGroups;

    public Integer getNo() { return no; }
    public void setNo(Integer no) { this.no = no; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFeatureGroupLabel() { return featureGroupLabel; }
    public void setFeatureGroupLabel(String featureGroupLabel) { this.featureGroupLabel = featureGroupLabel; }

    public List<IcecatFeatureGroup> getFeatureGroups() {
        return featureGroups != null ? featureGroups : Collections.emptyList();
    }

    public void setFeatureGroups(List<IcecatFeatureGroup> featureGroups) {
        this.featureGroups = featureGroups;
    }
}
