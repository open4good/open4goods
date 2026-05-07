package org.open4goods.icecat.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * Elasticsearch document representing an Icecat supplier (brand/manufacturer).
 *
 * <p>Populated from {@link IcecatSupplier} objects loaded via
 * {@link org.open4goods.icecat.services.loader.FeatureLoader}.
 * Provides persistent storage of brand logo URLs for use in brand enrichment.
 */
@Document(indexName = "icecat-suppliers", createIndex = true, writeTypeHint = WriteTypeHint.FALSE)
public class IcecatSupplierDocument {

    /** Icecat stable supplier ID. */
    @Id
    private Integer id;

    /** Brand/supplier name — searchable. */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    /** Best available logo URL (highest resolution). */
    @Field(type = FieldType.Keyword, index = false)
    private String logoUrl;

    /** High-resolution logo URL (may be null). */
    @Field(type = FieldType.Keyword, index = false)
    private String logoHighPic;

    /** Medium-resolution logo URL (may be null). */
    @Field(type = FieldType.Keyword, index = false)
    private String logoMediumPic;

    /** Low-resolution logo URL (may be null). */
    @Field(type = FieldType.Keyword, index = false)
    private String logoLowPic;

    /** Standard logo URL (may be null). */
    @Field(type = FieldType.Keyword, index = false)
    private String logoPic;

    public IcecatSupplierDocument() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getLogoHighPic() { return logoHighPic; }
    public void setLogoHighPic(String logoHighPic) { this.logoHighPic = logoHighPic; }

    public String getLogoMediumPic() { return logoMediumPic; }
    public void setLogoMediumPic(String logoMediumPic) { this.logoMediumPic = logoMediumPic; }

    public String getLogoLowPic() { return logoLowPic; }
    public void setLogoLowPic(String logoLowPic) { this.logoLowPic = logoLowPic; }

    public String getLogoPic() { return logoPic; }
    public void setLogoPic(String logoPic) { this.logoPic = logoPic; }
}
