package org.open4goods.model.icecat;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatResponse {
	@JacksonXmlProperty(isAttribute = true)
	private String Date;

	@JacksonXmlProperty(isAttribute = true)
	private String ID;

	@JacksonXmlProperty(isAttribute = true)
	private String Request_ID;

	@JacksonXmlProperty(isAttribute = true)
	private String Status;

	@JacksonXmlProperty(localName = "FeaturesList")
	private IcecatFeaturesList featuresList;

	@JacksonXmlProperty(localName = "SuppliersList")
	private IcecatSuppliersList suppliersList;
	
    @JacksonXmlProperty(localName = "CategoryFeaturesList")
    private CategoryFeaturesList categoryFeaturesList;
    
    
	public String getDate() {
		return Date;
	}

	public void setDate(String Date) {
		this.Date = Date;
	}

	public String getID() {
		return ID;
	}

	public void setID(String ID) {
		this.ID = ID;
	}

	public String getRequest_ID() {
		return Request_ID;
	}

	public void setRequest_ID(String Request_ID) {
		this.Request_ID = Request_ID;
	}

	public String getStatus() {
		return Status;
	}

	public void setStatus(String Status) {
		this.Status = Status;
	}

	public IcecatFeaturesList getFeaturesList() {
		return featuresList;
	}

	public void setFeaturesList(IcecatFeaturesList featuresList) {
		this.featuresList = featuresList;
	}

	public CategoryFeaturesList getCategoryFeaturesList() {
		return categoryFeaturesList;
	}

	public void setCategoryFeaturesList(CategoryFeaturesList categoryFeaturesList) {
		this.categoryFeaturesList = categoryFeaturesList;
	}

	public IcecatSuppliersList getSuppliersList() {
		return suppliersList;
	}

	public void setSuppliersList(IcecatSuppliersList suppliersList) {
		this.suppliersList = suppliersList;
	}
	
	
}