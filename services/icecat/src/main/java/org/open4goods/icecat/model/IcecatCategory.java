package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * A product category from the Icecat taxonomy.
 * Categories are hierarchical (via {@link #parentCategory}) and each carries a set of
 * {@link IcecatCategoryFeatureGroup}s that define which features apply to products in that category.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatCategory {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "UNCATID")
    private String uncatId;

    @JacksonXmlProperty(isAttribute = true, localName = "Searchable")
    private String searchable;

    @JacksonXmlProperty(isAttribute = true, localName = "Visible")
    private String visible;

    @JacksonXmlProperty(isAttribute = true, localName = "Score")
    private Integer score;

    @JacksonXmlProperty(isAttribute = true, localName = "ThumbPic")
    private String thumbPic;

    @JacksonXmlProperty(isAttribute = true, localName = "LowPic")
    private String lowPic;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "CategoryFeatureGroup")
    private List<IcecatCategoryFeatureGroup> categoryFeatureGroups;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Description")
    private List<IcecatDescription> descriptions;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Feature")
    private List<IcecatFeature> features;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Name")
    private List<IcecatName> names;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Names")
    private List<IcecatNames> namesList;

    @JacksonXmlProperty(localName = "ParentCategory")
    private IcecatParentCategory parentCategory;

    @Override
    public String toString() {
        return id + ":" + names;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUncatId() { return uncatId; }
    public void setUncatId(String uncatId) { this.uncatId = uncatId; }

    public String getSearchable() { return searchable; }
    public void setSearchable(String searchable) { this.searchable = searchable; }

    public String getVisible() { return visible; }
    public void setVisible(String visible) { this.visible = visible; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getThumbPic() { return thumbPic; }
    public void setThumbPic(String thumbPic) { this.thumbPic = thumbPic; }

    public String getLowPic() { return lowPic; }
    public void setLowPic(String lowPic) { this.lowPic = lowPic; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public List<IcecatCategoryFeatureGroup> getCategoryFeatureGroups() {
        return categoryFeatureGroups != null ? categoryFeatureGroups : Collections.emptyList();
    }
    public void setCategoryFeatureGroups(List<IcecatCategoryFeatureGroup> categoryFeatureGroups) {
        this.categoryFeatureGroups = categoryFeatureGroups;
    }

    public List<IcecatDescription> getDescriptions() {
        return descriptions != null ? descriptions : Collections.emptyList();
    }
    public void setDescriptions(List<IcecatDescription> descriptions) {
        this.descriptions = descriptions;
    }

    public List<IcecatFeature> getFeatures() {
        return features != null ? features : Collections.emptyList();
    }
    public void setFeatures(List<IcecatFeature> features) { this.features = features; }

    public List<IcecatName> getNames() {
        return names != null ? names : Collections.emptyList();
    }
    public void setNames(List<IcecatName> names) { this.names = names; }

    public List<IcecatNames> getNamesList() {
        return namesList != null ? namesList : Collections.emptyList();
    }
    public void setNamesList(List<IcecatNames> namesList) { this.namesList = namesList; }

    public IcecatParentCategory getParentCategory() { return parentCategory; }
    public void setParentCategory(IcecatParentCategory parentCategory) {
        this.parentCategory = parentCategory;
    }
}
