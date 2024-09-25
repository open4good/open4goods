package org.open4goods.commons.model.product;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.elasticsearch.annotations.Field;

public class ExternalIds {

	/**
	 * The amazon identifier
	 */
	@Field
	private String asin;
	
	/**
	 * The icecat identifier
	 */
	@Field
	private String icecat;
	
	
	/**
	 * Known mpn's
	 */
	@Field
	private Set<String> mpn = new HashSet<>();
	
	/**
	 * Known sku's
	 */
	@Field
	private Set<String> sku = new HashSet<>();

	public String getAsin() {
		return asin;
	}

	public void setAsin(String asin) {
		this.asin = asin;
	}

	public Set<String> getMpn() {
		return mpn;
	}

	public void setMpn(Set<String> mpn) {
		this.mpn = mpn;
	}

	public Set<String> getSku() {
		return sku;
	}

	public void setSku(Set<String> sku) {
		this.sku = sku;
	}

	public String getIcecat() {
		return icecat;
	}

	public void setIcecat(String icecat) {
		this.icecat = icecat;
	}
	
	
	
}
