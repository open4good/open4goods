package org.open4goods.commons.model.product;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.commons.model.BarcodeType;

public class MongoGtinInfo {


	/**
	 * As we convert barcode to long for efficienty, we can loose some distinction upon the initial
	 * barcodeformat. We srore it the type encountered before numeric conversion  
	 */
	private Set<BarcodeType> encounteredBarcodeType  = new HashSet<BarcodeType>();

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


	public Set<BarcodeType> getEncounteredBarcodeType() {
		return encounteredBarcodeType;
	}


	public void setEncounteredBarcodeType(Set<BarcodeType> encounteredBarcodeType) {
		this.encounteredBarcodeType = encounteredBarcodeType;
	}




}
