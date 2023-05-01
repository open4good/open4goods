package org.open4goods.ui.controllers.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.config.yml.attributes.AttributeConfig;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.product.AggregatedData;

/**
 * Results of a search inside a specific vertical
 * @author goulven
 *
 */
public class VerticalSearchResponse {
	

	private VerticalConfig verticalConfig;
	
	private VerticalSearchRequest request;
	
	private List<AggregatedData> data;

	
	private Integer maxOffers;

	private Integer minOffers;
	
	private Double minPrice = null;
	
	private Double maxPrice = null;
	
	private Long totalResults;
	
	private Integer from;
	
	private Integer to;

	
	private List<VerticalFilterTerm> conditions = new ArrayList<>();
	private List<VerticalFilterTerm> brands = new ArrayList<>();
	private List<VerticalFilterTerm> countries = new ArrayList<>();
	
	
	/** The custom aggregations, from filters **/
	private Map<AttributeConfig, List<VerticalFilterTerm>> customFilters = new HashMap<>();
	
	
	
	
	
	public List<AggregatedData> getData() {
		return data;
	}

	public void setData(List<AggregatedData> results) {
		this.data = results;
	}







	public VerticalSearchRequest getRequest() {
		return request;
	}

	public void setRequest(VerticalSearchRequest request) {
		this.request = request;
	}

	public Double getMinPrice() {
		return minPrice;
	}




	public void setMinPrice(Double min) {
		this.minPrice = min;
	}




	public Double getMaxPrice() {
		return maxPrice;
	}




	public void setMaxPrice(Double max) {
		this.maxPrice = max;
	}

	public Long getTotalResults() {
		return totalResults;
	}

	public void setTotalResults(Long totalResults) {
		this.totalResults = totalResults;
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

	


	public Integer getMaxOffers() {
		return maxOffers;
	}

	public void setMaxOffers(Integer maxOffers) {
		this.maxOffers = maxOffers;
	}

	public Integer getMinOffers() {
		return minOffers;
	}

	public void setMinOffers(Integer minOffers) {
		this.minOffers = minOffers;
	}

	public VerticalConfig getVerticalConfig() {
		return verticalConfig;
	}

	public void setVerticalConfig(VerticalConfig verticalConfig) {
		this.verticalConfig = verticalConfig;
	}

	public List<VerticalFilterTerm> getBrands() {
		return brands;
	}

	public void setBrands(List<VerticalFilterTerm> brands) {
		this.brands = brands;
	}

	public List<VerticalFilterTerm> getCountries() {
		return countries;
	}

	public void setCountries(List<VerticalFilterTerm> countries) {
		this.countries = countries;
	}

	public List<VerticalFilterTerm> getConditions() {
		return conditions;
	}

	public void setConditions(List<VerticalFilterTerm> conditions) {
		this.conditions = conditions;
	}

	public Map<AttributeConfig, List<VerticalFilterTerm>> getCustomFilters() {
		return customFilters;
	}

	public void setCustomFilters(Map<AttributeConfig, List<VerticalFilterTerm>> customFilters) {
		this.customFilters = customFilters;
	}

	
	
}
