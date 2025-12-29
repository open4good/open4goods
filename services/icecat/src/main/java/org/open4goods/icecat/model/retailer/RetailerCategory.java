package org.open4goods.icecat.model.retailer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a category from the Icecat Retailer API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetailerCategory {

    @JsonProperty("CategoryId")
    private Long categoryId;

    @JsonProperty("CategoryName")
    private String categoryName;

    @JsonProperty("ParentCategoryId")
    private Long parentCategoryId;

    @JsonProperty("Level")
    private Integer level;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Uncatid")
    private Long uncatId;

    @JsonProperty("Score")
    private Integer score;

    @JsonProperty("SearchScore")
    private Integer searchScore;

    @JsonProperty("LowPic")
    private String lowPic;

    @JsonProperty("ThumbPic")
    private String thumbPic;

    /**
     * Default constructor.
     */
    public RetailerCategory() {
    }

    /**
     * Full constructor.
     */
    public RetailerCategory(Long categoryId, String categoryName, Long parentCategoryId, Integer level, String description) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.parentCategoryId = parentCategoryId;
        this.level = level;
        this.description = description;
    }

    // Getters and setters

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(Long parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUncatId() {
        return uncatId;
    }

    public void setUncatId(Long uncatId) {
        this.uncatId = uncatId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getSearchScore() {
        return searchScore;
    }

    public void setSearchScore(Integer searchScore) {
        this.searchScore = searchScore;
    }

    public String getLowPic() {
        return lowPic;
    }

    public void setLowPic(String lowPic) {
        this.lowPic = lowPic;
    }

    public String getThumbPic() {
        return thumbPic;
    }

    public void setThumbPic(String thumbPic) {
        this.thumbPic = thumbPic;
    }

    @Override
    public String toString() {
        return "RetailerCategory{" +
                "categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", parentCategoryId=" + parentCategoryId +
                ", level=" + level +
                '}';
    }
}
