package org.open4goods.ui.controllers.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

	Integer pageNumber;
	Integer pageSize;

	Set<String> countries = new HashSet<>();
	
	List<NumericRangeFilter> numericFilters = new ArrayList<>();
	

	private String sortField;
	private org.elasticsearch.search.sort.SortOrder sortOrder;
	
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

	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(Integer from) {
		this.pageNumber = from;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer to) {
		this.pageSize = to;
	}

	public Set<String> getCountries() {
		return countries;
	}

	public void setCountries(Set<String> countries) {
		this.countries = countries;
	}

	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public org.elasticsearch.search.sort.SortOrder getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(org.elasticsearch.search.sort.SortOrder sortOrder) {
		this.sortOrder = sortOrder;
	}

	public List<NumericRangeFilter> getNumericFilters() {
		return numericFilters;
	}

	public void setNumericFilters(List<NumericRangeFilter> numericFilters) {
		this.numericFilters = numericFilters;
	}

	
}
