package org.open4goods.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BrandConfiguration {

	
	/** association of BRAND NAME <> LOGO IMAGE URL**/	
	private Map<String, String> brandsLogo = new HashMap<>();
	
	/**
	 * The brands to remove
	 */
	private Set<String> brandsToRemove = new HashSet<>();
	
	
	/**
	 * The brands to replace name with SOURCE BRAND NAME <> REPLACEMENT BRAND NAME
	 */
	private Map<String, String> brandsToReplace = new HashMap<>();


	public Map<String, String> getBrandsLogo() {
		return brandsLogo;
	}


	public void setBrandsLogo(Map<String, String> brandsLogo) {
		this.brandsLogo = brandsLogo;
	}


	public Set<String> getBrandsToRemove() {
		return brandsToRemove;
	}


	public void setBrandsToRemove(Set<String> brandsToRemove) {
		this.brandsToRemove = brandsToRemove;
	}


	public Map<String, String> getBrandsToReplace() {
		return brandsToReplace;
	}


	public void setBrandsToReplace(Map<String, String> brandsToReplace) {
		this.brandsToReplace = brandsToReplace;
	}
	
}
