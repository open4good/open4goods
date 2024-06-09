package org.open4goods.model.icecat;
import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(value = { "Versions","Keywords","VirtualCategories" })
public class IcecatCategory {
    @JacksonXmlProperty(isAttribute = true)
    private String UNCATID;

    @JacksonXmlProperty(isAttribute = true)
    private String Searchable;

    @JacksonXmlProperty(isAttribute = true)
    private String Visible;

    @JacksonXmlProperty(isAttribute = true)
    private Integer Score;

    @JacksonXmlProperty(isAttribute = true)
    private URI ThumbPic;

    @JacksonXmlProperty(isAttribute = true)
    private Integer ID;

    @JacksonXmlProperty(isAttribute = true)
    private URI LowPic;

    @JacksonXmlProperty(isAttribute = true)
    private String Updated;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "CategoryFeatureGroup")
    private List<IcecatCategoryFeatureGroup> categoryFeatureGroups;

//    @JacksonXmlElementWrapper(useWrapping = false)
//    @JacksonXmlProperty(localName = "Versions")
//    private List<Versions> versions;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Description")
    private List<IcecatDescription> descriptions;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Feature")
    private List<IcecatFeature> features;

//    @JacksonXmlElementWrapper(useWrapping = false)
//    @JacksonXmlProperty(localName = "Keywords")
//    private List<IcecatKeywords> keywords;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Name")
    private List<IcecatName> names;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Names")
    private List<IcecatNames> namesList;

    @JacksonXmlProperty(localName = "ParentCategory")
    private IcecatParentCategory parentCategory;

	public String getUNCATID() {
		return UNCATID;
	}

	public void setUNCATID(String uNCATID) {
		UNCATID = uNCATID;
	}

	public String getSearchable() {
		return Searchable;
	}

	public void setSearchable(String searchable) {
		Searchable = searchable;
	}



	public String getVisible() {
		return Visible;
	}

	public void setVisible(String visible) {
		Visible = visible;
	}

	public Integer getScore() {
		return Score;
	}

	public void setScore(Integer score) {
		Score = score;
	}

	public URI getThumbPic() {
		return ThumbPic;
	}

	public void setThumbPic(URI thumbPic) {
		ThumbPic = thumbPic;
	}

	public Integer getID() {
		return ID;
	}

	public void setID(Integer iD) {
		ID = iD;
	}

	public URI getLowPic() {
		return LowPic;
	}

	public void setLowPic(URI lowPic) {
		LowPic = lowPic;
	}

	public String getUpdated() {
		return Updated;
	}

	public void setUpdated(String updated) {
		Updated = updated;
	}

	public List<IcecatCategoryFeatureGroup> getCategoryFeatureGroups() {
		return categoryFeatureGroups;
	}

	public void setCategoryFeatureGroups(List<IcecatCategoryFeatureGroup> categoryFeatureGroups) {
		this.categoryFeatureGroups = categoryFeatureGroups;
	}

	public List<IcecatDescription> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<IcecatDescription> descriptions) {
		this.descriptions = descriptions;
	}

	public List<IcecatFeature> getFeatures() {
		return features;
	}

	public void setFeatures(List<IcecatFeature> features) {
		this.features = features;
	}

	public List<IcecatName> getNames() {
		return names;
	}

	public void setNames(List<IcecatName> names) {
		this.names = names;
	}

	public List<IcecatNames> getNamesList() {
		return namesList;
	}

	public void setNamesList(List<IcecatNames> namesList) {
		this.namesList = namesList;
	}

	public IcecatParentCategory getParentCategory() {
		return parentCategory;
	}

	public void setParentCategory(IcecatParentCategory parentCategory) {
		this.parentCategory = parentCategory;
	}

//    @JacksonXmlElementWrapper(useWrapping = false)
//    @JacksonXmlProperty(localName = "UNCATID")
//    private List<String> uncategorizedIds;

//    @JacksonXmlProperty(localName = "VirtualCategories")
//    private VirtualCategories virtualCategories;

}








