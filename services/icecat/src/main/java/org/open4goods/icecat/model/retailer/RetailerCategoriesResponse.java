package org.open4goods.icecat.model.retailer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response wrapper for the GetCategories endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetailerCategoriesResponse {

    @JsonProperty("Categories")
    private List<RetailerCategory> categories;

    @JsonProperty("TotalCount")
    private Integer totalCount;

    @JsonProperty("StatusCode")
    private Integer statusCode;

    @JsonProperty("Message")
    private String message;

    /**
     * Default constructor.
     */
    public RetailerCategoriesResponse() {
    }

    /**
     * Constructor with categories.
     */
    public RetailerCategoriesResponse(List<RetailerCategory> categories) {
        this.categories = categories;
        this.totalCount = categories != null ? categories.size() : 0;
    }

    // Getters and setters

    public List<RetailerCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<RetailerCategory> categories) {
        this.categories = categories;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
