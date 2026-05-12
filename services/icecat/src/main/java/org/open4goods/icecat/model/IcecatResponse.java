package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * The {@code <Response>} element wrapping all Icecat bulk XML data exports.
 * Different files populate different child elements; unused elements are null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatResponse {

    @JacksonXmlProperty(isAttribute = true, localName = "Date")
    private String date;

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private String id;

    @JacksonXmlProperty(isAttribute = true, localName = "Request_ID")
    private String requestId;

    @JacksonXmlProperty(isAttribute = true, localName = "Status")
    private String status;

    @JacksonXmlProperty(localName = "FeaturesList")
    private IcecatFeaturesList featuresList;

    @JacksonXmlProperty(localName = "FeatureGroupsList")
    private IcecatFeatureGroupsList featureGroupsList;

    @JacksonXmlProperty(localName = "SuppliersList")
    private IcecatSuppliersList suppliersList;

    @JacksonXmlProperty(localName = "CategoriesList")
    private IcecatCategoriesList categoryList;

    @JacksonXmlProperty(localName = "CategoryFeaturesList")
    private CategoryFeaturesList categoryFeaturesList;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public IcecatFeaturesList getFeaturesList() { return featuresList; }
    public void setFeaturesList(IcecatFeaturesList featuresList) { this.featuresList = featuresList; }

    public IcecatFeatureGroupsList getFeatureGroupsList() { return featureGroupsList; }
    public void setFeatureGroupsList(IcecatFeatureGroupsList featureGroupsList) {
        this.featureGroupsList = featureGroupsList;
    }

    public IcecatSuppliersList getSuppliersList() { return suppliersList; }
    public void setSuppliersList(IcecatSuppliersList suppliersList) { this.suppliersList = suppliersList; }

    public IcecatCategoriesList getCategoryList() { return categoryList; }
    public void setCategoryList(IcecatCategoriesList categoryList) { this.categoryList = categoryList; }

    public CategoryFeaturesList getCategoryFeaturesList() { return categoryFeaturesList; }
    public void setCategoryFeaturesList(CategoryFeaturesList categoryFeaturesList) {
        this.categoryFeaturesList = categoryFeaturesList;
    }
}
