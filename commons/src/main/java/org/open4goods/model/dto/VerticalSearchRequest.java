package org.open4goods.model.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.constants.ProductCondition;

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

	ProductCondition condition;

	Integer pageNumber;
	Integer pageSize;

	Set<String> countries = new HashSet<>();

	List<NumericRangeFilter> numericFilters = new ArrayList<>();

	Map<String,Set<String>> termsFilter = new HashMap<>();

	// If true, will search in vertical excluded products
	boolean excluded = false;

	private String sortField;
	private String sortOrder;


	/**
	 * Add a term filter
	 * @param string
	 * @param string2
	 */
	public void addTermFilter(String attribute, String term) {

		if (!termsFilter.containsKey(attribute)) {
			termsFilter.put(attribute, new HashSet<>());
		}
		termsFilter.get(attribute).add(term);
	}

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

	public ProductCondition getCondition() {
		return condition;
	}

	public void setCondition(ProductCondition condition) {
		this.condition = condition;
	}

	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(Integer from) {
		pageNumber = from;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer to) {
		pageSize = to;
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

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public List<NumericRangeFilter> getNumericFilters() {
		return numericFilters;
	}

	public void setNumericFilters(List<NumericRangeFilter> numericFilters) {
		this.numericFilters = numericFilters;
	}

	public Map<String, Set<String>> getTermsFilter() {
		return termsFilter;
	}

	public void setTermsFilter(Map<String, Set<String>> termsFilter) {
		this.termsFilter = termsFilter;
	}

	public boolean isExcluded() {
		return excluded;
	}

	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}

	

}
