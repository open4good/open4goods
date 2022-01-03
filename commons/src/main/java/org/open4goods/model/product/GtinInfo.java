package org.open4goods.model.product;

import org.open4goods.model.BarcodeType;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class GtinInfo {
	

	@Field(index = false, store = false, type = FieldType.Keyword)
	private BarcodeType upcType;
	
	/**
	 * Manufacturer country, from the gtin
	 */
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String country;



	public String getCountry() {
		return country;
	}


	public void setCountry(String country) {
		this.country = country;
	}


	public BarcodeType getUpcType() {
		return upcType;
	}


	public void setUpcType(BarcodeType upcType) {
		this.upcType = upcType;
	}



}
