package org.open4goods.icecat.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * Elasticsearch document representing an Icecat feature group
 * (a logical grouping such as "Display", "Connectivity", etc.).
 *
 * <p>Populated from {@link IcecatFeatureGroup} objects loaded via
 * {@link org.open4goods.icecat.services.loader.FeatureLoader}.
 */
@Document(indexName = "icecat-feature-groups", createIndex = true, writeTypeHint = WriteTypeHint.FALSE)
public class IcecatFeatureGroupDocument {

    /** Icecat stable feature group ID. */
    @Id
    private Integer id;

    /** English group name — primary full-text search field. */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String englishName;

    /**
     * All localized names encoded as {@code "langId:name"} strings.
     * Stored for display; not indexed for search.
     */
    @Field(type = FieldType.Keyword, index = false)
    private List<String> langNames;

    public IcecatFeatureGroupDocument() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }

    public List<String> getLangNames() { return langNames; }
    public void setLangNames(List<String> langNames) { this.langNames = langNames; }
}
