package org.open4goods.icecat.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Elasticsearch sub-document describing feature groups attached to a specific Icecat category.
 */
public class IcecatCategoryFeatureGroupDocument {

    /** Icecat category-feature group ID used by category features. */
    @Field(type = FieldType.Integer)
    private Integer id;

    /** Ordered feature-group IDs referenced by this category-feature group. */
    @Field(type = FieldType.Integer)
    private List<Integer> featureGroupIds = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public List<Integer> getFeatureGroupIds() { return featureGroupIds; }
    public void setFeatureGroupIds(List<Integer> featureGroupIds) { this.featureGroupIds = featureGroupIds; }
}
