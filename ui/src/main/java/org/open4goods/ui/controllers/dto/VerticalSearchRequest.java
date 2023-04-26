package org.open4goods.ui.controllers.dto;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.model.constants.ProductState;

/**
 * Results of a search inside a specific vertical
 * @author goulven
 *
 */
public class VerticalSearchRequest {
	
	
	Integer minPrice;
	Integer maxPrice;
	
	Integer minOffers;
	Integer maxOffers;

	ProductState condition;

	Integer from;
	Integer to;

	Set<String> countries = new HashSet<>();

	public Integer getMinPrice() {
		return minPrice;
	}

	public void setMinPrice(Integer minPrice) {
		this.minPrice = minPrice;
	}

	public Integer getMaxPrice() {
		return maxPrice;
	}

	public void setMaxPrice(Integer maxPrice) {
		this.maxPrice = maxPrice;
	}

	

	public Integer getMinOffers() {
		return minOffers;
	}

	public void setMinOffers(Integer minOffers) {
		this.minOffers = minOffers;
	}

	public Integer getMaxOffers() {
		return maxOffers;
	}

	public void setMaxOffers(Integer maxOffers) {
		this.maxOffers = maxOffers;
	}

	public ProductState getCondition() {
		return condition;
	}

	public void setCondition(ProductState condition) {
		this.condition = condition;
	}

	public Integer getFrom() {
		return from;
	}

	public void setFrom(Integer from) {
		this.from = from;
	}

	public Integer getTo() {
		return to;
	}

	public void setTo(Integer to) {
		this.to = to;
	}

	public Set<String> getCountries() {
		return countries;
	}

	public void setCountries(Set<String> countries) {
		this.countries = countries;
	}
	
	
	
	
	
	
	
	
	
	
}
