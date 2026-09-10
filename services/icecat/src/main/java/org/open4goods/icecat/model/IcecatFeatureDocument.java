package org.open4goods.icecat.model;

import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * Elasticsearch document representing an Icecat feature (specification attribute).
 *
 * <p>Populated from {@link org.open4goods.icecat.jaxb.Feature} objects loaded via {@link org.open4goods.icecat.services.loader.FeatureLoader}.
 * Used as a persistent backing store and to power admin search and attribute-resolution endpoints.
 */
@Document(indexName = "icecat-features", createIndex = false, writeTypeHint = WriteTypeHint.FALSE)
public class IcecatFeatureDocument {

    /** Icecat stable feature ID. */
    @Id
    private Integer id;

    /** Feature data type (e.g. "numerical", "YES/NO"). */
    @Field(type = FieldType.Keyword)
    private String type;

    /** English name (langId=1) — primary full-text search field. */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String englishName;

    /**
     * Normalized attribute names (all languages) used for ES-backed attribute resolution.
     */
    @Field(type = FieldType.Keyword)
    private Set<String> normalizedNames;

    /**
     * All localized names encoded as {@code "langId:name"} strings.
     * Stored for localized display-name lookup; not indexed for search.
     */
    @Field(type = FieldType.Keyword, index = false)
    private List<String> langNames;

    public IcecatFeatureDocument() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }

    public Set<String> getNormalizedNames() { return normalizedNames; }
    public void setNormalizedNames(Set<String> normalizedNames) { this.normalizedNames = normalizedNames; }

    public List<String> getLangNames() { return langNames; }
    public void setLangNames(List<String> langNames) { this.langNames = langNames; }

    /**
     * Returns the name for the given Icecat language ID, decoding the {@code "langId:name"}
     * encoding used by {@link #langNames}.
     *
     * @param languageId Icecat language ID (see {@link org.open4goods.icecat.util.IcecatConstants})
     * @return the localized name, or {@code null} if this feature has no name in that language
     */
    public String localizedName(int languageId) {
        if (langNames == null) {
            return null;
        }
        String prefix = languageId + ":";
        for (String entry : langNames) {
            if (entry.startsWith(prefix)) {
                return entry.substring(prefix.length());
            }
        }
        return null;
    }
}
