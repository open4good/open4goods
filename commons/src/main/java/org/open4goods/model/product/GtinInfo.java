package org.open4goods.model.product;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class GtinInfo {
	
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



}
