package org.open4goods.ui.controllers.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.model.product.AggregatedData;

/**
 * Results of a search inside a specific vertical
 * @author goulven
 *
 */
public class VerticalSearchResponse {
	
	private String verticalName;
	
	private List<AggregatedData> data;

	private Map<String, Integer> brands = new HashMap<>();
	
	private Integer maxOffers;

	private Integer minOffers;
	
	private Double minPrice = null;
	
	private Double maxPrice = null;
	
	private long totalResults;
	
	private long from;
	
	private long to;

	private long itemNew;
	
	private long itemOccasion;

	private long itemUnknown;
	
	private Map<String, Long> categories = new HashMap<>();
	
	
	
	
	
	public String getVerticalName() {
		return verticalName;
	}

	public void setVerticalName(String verticalName) {
		this.verticalName = verticalName;
	}

	public List<AggregatedData> getData() {
		return data;
	}

	public void setData(List<AggregatedData> results) {
		this.data = results;
	}




	public Map<String, Integer> getBrands() {
		return brands;
	}




	public void setBrands(Map<String, Integer> brands) {
		this.brands = brands;
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




	public long getTotalResults() {
		return totalResults;
	}




	public void setTotalResults(long totalResults) {
		this.totalResults = totalResults;
	}


	public long getFrom() {
		return from;
	}

	public void setFrom(long from) {
		this.from = from;
	}

	public long getTo() {
		return to;
	}

	public void setTo(long to) {
		this.to = to;
	}

	public long getItemNew() {
		return itemNew;
	}

	public void setItemNew(long itemNew) {
		this.itemNew = itemNew;
	}

	public long getItemOccasion() {
		return itemOccasion;
	}

	public void setItemOccasion(long itemOccasion) {
		this.itemOccasion = itemOccasion;
	}

	public long getItemUnknown() {
		return itemUnknown;
	}

	public void setItemUnknown(long itemUnknown) {
		this.itemUnknown = itemUnknown;
	}

	public Map<String, Long> getCategories() {
		return categories;
	}

	public void setCategories(Map<String, Long> categories) {
		this.categories = categories;
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
	
	
	
	
}
