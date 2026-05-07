package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * A product feature (specification) definition from the Icecat catalog.
 * Features have a unique ID, a data type, optional unit of measure, and multilingual names.
 * The feature ID is stable across Icecat data exports and the live API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatFeature {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "Type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    @JacksonXmlProperty(isAttribute = true, localName = "CategoryFeatureGroup_ID")
    private int categoryFeatureGroupId;

    @JacksonXmlProperty(isAttribute = true, localName = "CategoryFeature_ID")
    private int categoryFeatureId;

    @JacksonXmlProperty(isAttribute = true, localName = "LimitDirection")
    private int limitDirection;

    @JacksonXmlProperty(isAttribute = true, localName = "Mandatory")
    private int mandatory;

    @JacksonXmlProperty(isAttribute = true, localName = "Searchable")
    private int searchable;

    @JacksonXmlProperty(isAttribute = true, localName = "No")
    private String no;

    @JacksonXmlProperty(isAttribute = true, localName = "Class")
    private String clazz;

    @JacksonXmlProperty(isAttribute = true, localName = "DefaultDisplayUnit")
    private String defaultDisplayUnit;

    @JacksonXmlProperty(isAttribute = true, localName = "Use_Dropdown_Input")
    private String useDropdownInput;

    @JacksonXmlProperty(isAttribute = true, localName = "ValueSorting")
    private int valueSorting;

    @JacksonXmlProperty(localName = "Descriptions")
    private IcecatDescriptions descriptions;

    @JacksonXmlProperty(localName = "Measure")
    private IcecatMeasure measure;

    @JacksonXmlProperty(localName = "Names")
    private IcecatNames names;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public int getCategoryFeatureGroupId() { return categoryFeatureGroupId; }
    public void setCategoryFeatureGroupId(int categoryFeatureGroupId) { this.categoryFeatureGroupId = categoryFeatureGroupId; }

    public int getCategoryFeatureId() { return categoryFeatureId; }
    public void setCategoryFeatureId(int categoryFeatureId) { this.categoryFeatureId = categoryFeatureId; }

    public int getLimitDirection() { return limitDirection; }
    public void setLimitDirection(int limitDirection) { this.limitDirection = limitDirection; }

    public int getMandatory() { return mandatory; }
    public void setMandatory(int mandatory) { this.mandatory = mandatory; }

    public int getSearchable() { return searchable; }
    public void setSearchable(int searchable) { this.searchable = searchable; }

    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }

    public String getClazz() { return clazz; }
    public void setClazz(String clazz) { this.clazz = clazz; }

    public String getDefaultDisplayUnit() { return defaultDisplayUnit; }
    public void setDefaultDisplayUnit(String defaultDisplayUnit) { this.defaultDisplayUnit = defaultDisplayUnit; }

    public String getUseDropdownInput() { return useDropdownInput; }
    public void setUseDropdownInput(String useDropdownInput) { this.useDropdownInput = useDropdownInput; }

    public int getValueSorting() { return valueSorting; }
    public void setValueSorting(int valueSorting) { this.valueSorting = valueSorting; }

    public IcecatDescriptions getDescriptions() { return descriptions; }
    public void setDescriptions(IcecatDescriptions descriptions) { this.descriptions = descriptions; }

    public IcecatMeasure getMeasure() { return measure; }
    public void setMeasure(IcecatMeasure measure) { this.measure = measure; }

    public IcecatNames getNames() { return names; }
    public void setNames(IcecatNames names) { this.names = names; }
}
