package org.open4goods.icecat.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * Elasticsearch document representing an Icecat product category.
 *
 * <p>Populated from {@link IcecatCategory} objects loaded via
 * {@link org.open4goods.icecat.services.loader.CategoryLoader}.
 * Used for admin category-browsing and fuzzy vertical-to-category matching.
 */
@Document(indexName = "icecat-categories", createIndex = true, writeTypeHint = WriteTypeHint.FALSE)
public class IcecatCategoryDocument {

    /** Icecat stable category ID. */
    @Id
    private Integer id;

    /** English category name — primary full-text search field for fuzzy matching. */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String englishName;

    /** Parent category ID (null for root categories). */
    @Field(type = FieldType.Integer)
    private Integer parentId;

    /** Icecat relevance score for this category. */
    @Field(type = FieldType.Integer)
    private Integer score;

    /**
     * All localized names encoded as {@code "langId:name"} strings.
     * Stored for display; not indexed for search.
     */
    @Field(type = FieldType.Keyword, index = false)
    private List<String> langNames;

    /**
     * Feature groups available for this category, loaded from the Icecat
     * {@code CategoryFeaturesList.xml} reference export.
     */
    @Field(type = FieldType.Object)
    private List<IcecatCategoryFeatureGroupDocument> featureGroups = new ArrayList<>();

    /**
     * Features available for this category, with category-specific metadata.
     */
    @Field(type = FieldType.Object)
    private List<IcecatCategoryFeatureDocument> features = new ArrayList<>();

    public IcecatCategoryDocument() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public List<String> getLangNames() { return langNames; }
    public void setLangNames(List<String> langNames) { this.langNames = langNames; }

    public List<IcecatCategoryFeatureGroupDocument> getFeatureGroups() { return featureGroups; }
    public void setFeatureGroups(List<IcecatCategoryFeatureGroupDocument> featureGroups) {
        this.featureGroups = featureGroups;
    }

    public List<IcecatCategoryFeatureDocument> getFeatures() { return features; }
    public void setFeatures(List<IcecatCategoryFeatureDocument> features) { this.features = features; }
}
