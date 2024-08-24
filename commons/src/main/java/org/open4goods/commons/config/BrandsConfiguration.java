package org.open4goods.commons.config;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.commons.model.data.Brand;

public class BrandsConfiguration {

	
	private Set<Brand> brands = new HashSet<>();
	
	/**
	 * The brands to remove
	 */
	private Set<String> brandsToRemove = new HashSet<>();

	public Set<String> getBrandsToRemove() {
		return brandsToRemove;
	}


	public void setBrandsToRemove(Set<String> brandsToRemove) {
		this.brandsToRemove = brandsToRemove;
	}


	public Set<Brand> getBrands() {
		return brands;
	}


	public void setBrands(Set<Brand> brands) {
		this.brands = brands;
	}

	
}
