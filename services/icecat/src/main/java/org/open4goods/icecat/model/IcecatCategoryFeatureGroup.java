package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

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

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "FeatureGroup")
    private List<IcecatFeatureGroup> featureGroups;

    public Integer getNo() { return no; }
    public void setNo(Integer no) { this.no = no; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public List<IcecatFeatureGroup> getFeatureGroups() {
        return featureGroups != null ? featureGroups : Collections.emptyList();
    }

    public void setFeatureGroups(List<IcecatFeatureGroup> featureGroups) {
        this.featureGroups = featureGroups;
    }
}
