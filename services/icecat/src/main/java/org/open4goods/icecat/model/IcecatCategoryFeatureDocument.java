package org.open4goods.icecat.model;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Elasticsearch sub-document describing a feature as it is attached to a specific Icecat category.
 */
public class IcecatCategoryFeatureDocument {

    /** Icecat stable feature ID. */
    @Field(type = FieldType.Integer)
    private Integer id;

    /** Icecat category-feature group ID used inside {@code CategoryFeaturesList.xml}. */
    @Field(type = FieldType.Integer)
    private Integer categoryFeatureGroupId;

    /** Icecat category-feature relation ID. */
    @Field(type = FieldType.Integer)
    private Integer categoryFeatureId;

    /** Category-specific feature type if present in the category export. */
    @Field(type = FieldType.Keyword)
    private String type;

    /** Display order from Icecat. */
    @Field(type = FieldType.Keyword)
    private String no;

    /** Icecat feature class. */
    @Field(type = FieldType.Keyword)
    private String clazz;

    /** Default display unit in the category context. */
    @Field(type = FieldType.Keyword)
    private String defaultDisplayUnit;

    /** Limit direction flag from Icecat. */
    @Field(type = FieldType.Integer)
    private Integer limitDirection;

    /** Mandatory flag from Icecat. */
    @Field(type = FieldType.Integer)
    private Integer mandatory;

    /** Searchable flag from Icecat. */
    @Field(type = FieldType.Integer)
    private Integer searchable;

    /** Dropdown-input flag from Icecat. */
    @Field(type = FieldType.Keyword)
    private String useDropdownInput;

    /** Value sorting flag from Icecat. */
    @Field(type = FieldType.Integer)
    private Integer valueSorting;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCategoryFeatureGroupId() { return categoryFeatureGroupId; }
    public void setCategoryFeatureGroupId(Integer categoryFeatureGroupId) {
        this.categoryFeatureGroupId = categoryFeatureGroupId;
    }

    public Integer getCategoryFeatureId() { return categoryFeatureId; }
    public void setCategoryFeatureId(Integer categoryFeatureId) { this.categoryFeatureId = categoryFeatureId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }

    public String getClazz() { return clazz; }
    public void setClazz(String clazz) { this.clazz = clazz; }

    public String getDefaultDisplayUnit() { return defaultDisplayUnit; }
    public void setDefaultDisplayUnit(String defaultDisplayUnit) { this.defaultDisplayUnit = defaultDisplayUnit; }

    public Integer getLimitDirection() { return limitDirection; }
    public void setLimitDirection(Integer limitDirection) { this.limitDirection = limitDirection; }

    public Integer getMandatory() { return mandatory; }
    public void setMandatory(Integer mandatory) { this.mandatory = mandatory; }

    public Integer getSearchable() { return searchable; }
    public void setSearchable(Integer searchable) { this.searchable = searchable; }

    public String getUseDropdownInput() { return useDropdownInput; }
    public void setUseDropdownInput(String useDropdownInput) { this.useDropdownInput = useDropdownInput; }

    public Integer getValueSorting() { return valueSorting; }
    public void setValueSorting(Integer valueSorting) { this.valueSorting = valueSorting; }
}
