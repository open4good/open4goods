package org.open4goods.commons.model.product;

import org.open4goods.commons.model.BarcodeType;

public class GtinInfo {


	private BarcodeType upcType;

	/**
	 * Manufacturer country, from the gtin
	 */
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
