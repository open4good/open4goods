package org.open4goods.config.yml.ui;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonMerge;

public class LogosConfig {

	/** Informations about brands present in the system **/
	@JsonMerge
	private Map<String, String> brandsLogo = new HashMap<>();

	/** Informations about stores present in the system **/
	@JsonMerge
	private Map<String, String> storesLogo = new HashMap<>();

	public Map<String, String> getBrandsLogo() {
		return brandsLogo;
	}

	public void setBrandsLogo(Map<String, String> brandsLogo) {
		this.brandsLogo = brandsLogo;
	}

	public Map<String, String> getStoresLogo() {
		return storesLogo;
	}

	public void setStoresLogo(Map<String, String> storesLogo) {
		this.storesLogo = storesLogo;
	}




}
