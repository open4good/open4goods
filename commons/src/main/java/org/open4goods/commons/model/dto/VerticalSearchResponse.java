package org.open4goods.commons.model.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.commons.config.yml.attributes.AttributeConfig;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.model.product.Product;

/**
 * Results of a search inside a specific vertical
 * @author goulven
 *
 */
public class VerticalSearchResponse {


	private VerticalConfig verticalConfig;

	private VerticalSearchRequest request;

	private List<Product> data;


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
	private Map<String,NumericRangeFilter> numericFilters = new HashMap<>();
	
	


	/** The custom aggregations, pageNumber filters **/
	private Map<AttributeConfig, List<VerticalFilterTerm>> customFilters = new HashMap<>();





	public VerticalSearchResponse(VerticalConfig config, VerticalSearchRequest vRequest) {
		this.data = new ArrayList<>();
		this.to=0;
		this.from=0;
		this.totalResults=0L;
		this.minOffers=0;
		this.maxOffers=0;
		this.verticalConfig=config;
		this.request=vRequest;
	}

	@Override
	public String toString() {
		return totalResults + " results";
	}
	
	
	public VerticalSearchResponse() {
	}

	public List<Product> limitedDatas(Integer to) {
		if (to > data.size()) {
			return data;
		}
		return data.subList(0, to);
	}
	
	public List<Product> getData() {
		return data;
	}

	public void setData(List<Product> results) {
		data = results;
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
		minPrice = min;
	}




	public Double getMaxPrice() {
		return maxPrice;
	}




	public void setMaxPrice(Double max) {
		maxPrice = max;
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

	public Map<String, NumericRangeFilter> getNumericFilters() {
		return numericFilters;
	}

	public void setNumericFilters(Map<String, NumericRangeFilter> numericFilters) {
		this.numericFilters = numericFilters;
	}



}
