package org.open4goods.brand.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.open4goods.model.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Document(indexName = "brand-scores", createIndex = true, writeTypeHint = WriteTypeHint.FALSE)
public class BrandScore {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrandScore.class);

    @Id
    private String id;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private long lastUpdate;

    @Field(index = true, store = false, type = FieldType.Keyword)
    private String datasourceName;

    @Field(index = true, store = false, type = FieldType.Text)
    private String brandName;

    @Field(index = true, store = false, type = FieldType.Keyword)
    private String scoreValue;

    @Field(index = true, store = false, type = FieldType.Double)
    private Double normalized;

    @Field(index = true, store = false, type = FieldType.Keyword)
    private Set<String> tags = new HashSet<>();

    @Field(index = true, store = false, type = FieldType.Keyword)
    private String url;

    public BrandScore() {
    }

    public BrandScore(String datasourceName, Double invertScaleBase, String brandName, String scoreValue, String url) {
        this.datasourceName = Objects.requireNonNull(datasourceName, "datasourceName");
        this.brandName = brandName == null ? null : brandName.toLowerCase().trim();
        this.id = id(datasourceName, brandName);
        this.lastUpdate = System.currentTimeMillis();
        this.scoreValue = scoreValue;
        this.url = url;
        this.normalized = normalizeScore(scoreValue, invertScaleBase, brandName);
    }

    public BrandScore(String id) {
        this.id = id;
    }

    private Double normalizeScore(String scoreValue, Double invertScaleBase, String brandName) {
        if (scoreValue == null) {
            return null;
        }
        try {
            Double value = Double.valueOf(scoreValue);
            if (invertScaleBase != null) {
                Double normalizedValue = invertScaleBase - value;
                LOGGER.info("Normalized score for brand {} with score {} is {}", brandName, scoreValue, normalizedValue);
                return normalizedValue;
            }
            LOGGER.info("Normalized score for brand {} with score {} is {}", brandName, scoreValue, value);
            return value;
        } catch (NumberFormatException e) {
            LOGGER.error("Error with score normalization for brand {} with score {}", brandName, scoreValue, e);
            return null;
        }
    }

    public static String id(String datasourceName, String brandName) {
        return IdHelper.azCharAndDigits(datasourceName) + "-" + IdHelper.azCharAndDigits(brandName).toLowerCase();
    }

    @Override
    public String toString() {
        return datasourceName + "-" + brandName + "=" + scoreValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(String scoreValue) {
        this.scoreValue = scoreValue;
    }

    public Double getNormalized() {
        return normalized;
    }

    public void setNormalized(Double normalized) {
        this.normalized = normalized;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
